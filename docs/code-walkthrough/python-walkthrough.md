# Python 版代码逐行讲解

> 本文档面向小白，逐步讲解 Python 版的核心代码逻辑。建议搭配代码一起阅读。

---

## 目录

1. [项目结构总览](#1-项目结构总览)
2. [数据模型 (schemas.py)](#2-数据模型)
3. [配置管理 (config.py)](#3-配置管理)
4. [Monitor Agent 详解](#4-monitor-agent-详解)
5. [Research Agent 详解](#5-research-agent-详解)
6. [Compare Agent 详解](#6-compare-agent-详解)
7. [Battlecard Agent 详解](#7-battlecard-agent-详解)
8. [Alert Agent 详解](#8-alert-agent-详解)
9. [LangGraph 工作流 (workflow.py)](#9-langgraph-工作流)
10. [FastAPI 服务 (server.py)](#10-fastapi-服务)

---

## 1. 项目结构总览

```
python/
├── src/
│   ├── agents/          # 5个Agent的实现
│   │   ├── monitor_agent.py    ← 监控Agent
│   │   ├── research_agent.py   ← 研究Agent
│   │   ├── compare_agent.py    ← 对比Agent
│   │   ├── battlecard_agent.py ← 战术卡Agent
│   │   └── alert_agent.py      ← 预警Agent
│   ├── graph/
│   │   └── workflow.py   ← LangGraph 工作流编排（核心！）
│   ├── tools/            # 工具层
│   │   ├── web_scraper.py     ← 网页爬取
│   │   ├── search_tool.py     ← 搜索API
│   │   └── notification.py    ← 通知推送
│   ├── models/
│   │   └── schemas.py    ← 所有数据模型定义
│   ├── api/
│   │   └── server.py     ← FastAPI HTTP 服务
│   └── config.py         ← 配置管理
├── tests/                # 测试
├── requirements.txt      # Python 依赖
├── Dockerfile           # Docker镜像
├── docker-compose.yml   # 一键部署
└── .env.example         # 配置模板
```

**代码阅读顺序建议**：schemas.py → config.py → 工具层 → Agent层 → workflow.py → server.py

---

## 2. 数据模型

文件：`src/models/schemas.py`

这是整个系统的"数据字典"，定义了所有Agent之间传递的数据结构。

### CompetitorChange — Monitor Agent 的输出

```python
class CompetitorChange(BaseModel):
    competitor: str              # 竞品名称，如 "Stripe"
    change_type: ChangeType      # 变化类型（枚举：pricing/product/hiring/news）
    title: str                   # 变化标题，如 "Pro plan price increased"
    summary: str                 # 变化摘要
    url: str = ""                # 来源URL
    severity: Severity = Severity.MEDIUM  # 严重程度（low/medium/high/critical）
    detected_at: datetime        # 检测时间
```

**为什么用 Pydantic BaseModel？**
- 自动类型校验：如果传入 `severity="invalid"`，会自动报错
- 自动序列化：`.model_dump()` 转dict，`.model_dump_json()` 转JSON
- 文档自动生成：FastAPI 会自动生成API文档

### CIState — Pipeline 状态

```python
class CIState(BaseModel):
    competitor: str
    changes_detected: list[CompetitorChange] = []  # Monitor的输出
    research_results: list[ResearchInsight] = []    # Research的输出
    comparison_matrix: Optional[ComparisonMatrix]    # Compare的输出
    battlecard: Optional[Battlecard]                 # Battlecard的输出
    alerts_sent: list[Alert] = []                    # Alert的输出
    quality_score: float = 0.0                       # Quality Check的评分
    reflexion_count: int = 0                         # 重试次数
```

这个 State 对象在整个 Pipeline 中流转，每个 Agent 读取需要的字段、写入自己的输出。

---

## 3. 配置管理

文件：`src/config.py`

使用 dataclass + 环境变量的方式管理配置：

```python
@dataclass(frozen=True)        # frozen=True 表示配置不可修改
class LLMConfig:
    provider: str = os.getenv("LLM_PROVIDER", "openai")  # 从环境变量读取，有默认值
    model: str = os.getenv("LLM_MODEL", "gpt-4o")
    api_key: str = os.getenv("OPENAI_API_KEY", "")
```

**为什么用 `frozen=True`？** 配置在启动时加载一次就不应该被修改，frozen 防止意外修改。

---

## 4. Monitor Agent 详解

文件：`src/agents/monitor_agent.py`

### 核心流程

```
输入: competitor名称 + URL列表
  ↓
对每个URL:
  1. fetch_page(url) → HTML内容
  2. content_hash(HTML) → SHA-256哈希
  3. 对比Redis中的旧哈希
  4. 如果不同 → extract_text + extract_pricing + extract_jobs
  5. 将提取内容发给LLM分析
  ↓
输出: CompetitorChange列表
```

### 关键代码解读

**LLM交互**：
```python
response = await self.llm.ainvoke([
    SystemMessage(content=SYSTEM_PROMPT),   # 告诉LLM它的角色和任务
    HumanMessage(content=user_msg),         # 提供具体的网页内容
])
```

`ainvoke` 是异步调用（前缀 `a` = async），不会阻塞其他请求。

**LangGraph 节点接口**：
```python
async def __call__(self, state: dict) -> dict:
    # 从state读取输入
    competitor = state["competitor"]
    # 执行逻辑
    changes = await self.detect_changes(...)
    # 返回要更新的字段
    return {"changes_detected": [c.model_dump() for c in changes]}
```

每个Agent都实现 `__call__` 方法，这样可以直接作为LangGraph的节点函数。

---

## 5. Research Agent 详解

文件：`src/agents/research_agent.py`

### 核心流程

```
输入: competitor + 检测到的变化列表
  ↓
1. 并行搜索5个维度的信息:
   - 财务数据
   - 专利信息
   - 技术博客
   - 开源动态
   - 战略新闻
2. 汇总搜索结果
3. 交给LLM做深度分析
  ↓
输出: ResearchInsight列表
```

### RAG 模式

```python
async def _gather_intelligence(self, competitor):
    queries = [
        f"{competitor} financial results revenue 2026",
        f"{competitor} patent filings technology",
        ...
    ]
    results = {}
    for q in queries:
        results[q] = await web_search(q)  # 检索
    return results
```

先检索（Retrieve），再把检索结果放入prompt让LLM分析（Augment + Generate）。

---

## 6. Compare Agent 详解

文件：`src/agents/compare_agent.py`

8个固定维度评分：Product Features, Pricing, UX, Market Share, Sentiment, Technology, Ecosystem, Support。

每个维度给"我们"和"竞品"各打0-10分，附带说明。这样保证不同竞品的对比是标准化的。

---

## 7. Battlecard Agent 详解

文件：`src/agents/battlecard_agent.py`

输出一张完整的销售战术卡：

```json
{
  "our_strengths": ["更好的API文档", "更低的定价"],
  "competitor_weaknesses": ["客服响应慢"],
  "objection_handling": {
    "他们说竞品更便宜": "我们的TCO更低因为...",
    "竞品功能更多": "我们专注核心功能，稳定性更高..."
  },
  "elevator_pitch": "相比竞品X，我们提供更低的价格、更好的开发体验..."
}
```

---

## 8. Alert Agent 详解

文件：`src/agents/alert_agent.py`

最简单的Agent——不需要LLM，只是过滤 HIGH/CRITICAL 级别的变化并推送。

```python
critical_changes = [
    c for c in changes
    if c.severity in (Severity.HIGH, Severity.CRITICAL)
]
```

---

## 9. LangGraph 工作流

文件：`src/graph/workflow.py` — **这是全项目最核心的文件**

### State 定义（带Reducer）

```python
class PipelineState(TypedDict, total=False):
    changes_detected: Annotated[list, _merge_lists]  # 追加模式
    comparison_matrix: dict                            # 覆盖模式
```

`Annotated[list, _merge_lists]` 的含义：当多个节点都往 `changes_detected` 写数据时，不是覆盖，而是追加。

### 构建图

```python
graph = StateGraph(PipelineState)

# 添加5个节点（Agent）
graph.add_node("monitor", monitor_agent)
graph.add_node("alert", alert_agent)
graph.add_node("research", research_agent)
graph.add_node("compare", compare_agent)
graph.add_node("battlecard", battlecard_agent)
graph.add_node("quality_check", quality_check)

# 入口
graph.set_entry_point("monitor")

# 并行扇出：Monitor同时触发Alert和Research
graph.add_edge("monitor", "alert")
graph.add_edge("monitor", "research")

# Pipeline顺序流
graph.add_edge("research", "compare")
graph.add_edge("compare", "battlecard")
graph.add_edge("battlecard", "quality_check")

# Reflexion条件分支
graph.add_conditional_edges("quality_check", _should_retry)

# Alert独立结束
graph.add_edge("alert", END)
```

### Reflexion 实现

```python
def _should_retry(state):
    if state["quality_score"] < 7.0 and state["reflexion_count"] < 3:
        return "research"  # 回到Research重做
    return END             # 质量达标，结束
```

---

## 10. FastAPI 服务

文件：`src/api/server.py`

### 同步分析接口

```python
@app.post("/analyze")
async def analyze(req: AnalyzeRequest):
    final = await pipeline.ainvoke(initial_state)  # 运行整个Pipeline
    return AnalyzeResponse(...)
```

### SSE 流式接口

```python
@app.post("/analyze/stream")
async def analyze_stream(req: AnalyzeRequest):
    async def event_generator():
        async for event in pipeline.astream(initial_state):
            for node_name, output in event.items():
                yield {"event": node_name, "data": json.dumps(output)}
    return EventSourceResponse(event_generator())
```

`astream` 会在每个Agent执行完后立即推送一个SSE事件，前端可以实时展示进度。

---

## 下一步阅读

- [Java 版代码讲解](./java-walkthrough.md)
- [Go 版代码讲解](./go-walkthrough.md)
- [架构设计文档](../architecture.md)
- [八股文题库](../interview/eight-part-essay.md)
