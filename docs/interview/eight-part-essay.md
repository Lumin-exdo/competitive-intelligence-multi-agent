# 多Agent系统面试八股文题库（50+ 题）

> 本文档覆盖多Agent竞品情报系统面试中的高频知识点，从基础概念到生产实践，帮助你系统性准备面试。

---

## 一、Agent 基础概念（10题）

### 1. 什么是AI Agent？它和传统聊天机器人有什么区别？

**答：** AI Agent 是一个具备自主决策能力的智能体，核心架构包含四个组件：
- **LLM（大语言模型）**：作为"大脑"，负责推理和决策
- **Planning（规划）**：将复杂任务分解为可执行步骤
- **Memory（记忆）**：短期记忆（上下文窗口）+ 长期记忆（向量数据库）
- **Tool Use（工具使用）**：调用外部API、数据库、搜索引擎等

与传统聊天机器人的关键区别：
| 维度 | 传统聊天机器人 | AI Agent |
|------|--------------|----------|
| 决策方式 | 规则/意图匹配 | LLM自主推理 |
| 任务复杂度 | 单轮问答 | 多步骤复杂任务 |
| 工具调用 | 预定义流程 | 动态选择工具 |
| 适应性 | 固定流程 | 根据上下文调整策略 |

### 2. 什么是 ReAct 模式？为什么它是Agent的核心范式？

**答：** ReAct（Reasoning + Acting）是让Agent在推理和行动之间交替进行的模式：

```
Thought: 我需要查询竞品A的最新定价
Action: 调用web_scraper工具爬取pricing页面
Observation: 获取到定价数据...
Thought: 价格从$49涨到$59，这是一个重要变化
Action: 调用alert工具推送通知
```

核心价值：
1. **可解释性**：每一步决策都有明确的reasoning trace
2. **可纠错**：在observation阶段可以发现错误并调整
3. **可扩展**：新工具只需要注册，不需要修改推理逻辑

### 3. ReAct vs CoT vs ToT 的区别？

**答：**
- **CoT（Chain of Thought）**：纯推理链，不与外部环境交互。适合数学/逻辑推理
- **ReAct**：推理+行动交替，可以调用工具获取外部信息。适合需要实时数据的任务
- **ToT（Tree of Thought）**：树状搜索多条推理路径，选最优解。适合需要探索的创意/策略任务

在竞品情报系统中选择 ReAct 的原因：需要频繁调用搜索、爬虫等外部工具获取实时数据。

### 4. 什么是 Function Calling？和 Tool Use 有什么关系？

**答：** Function Calling 是 OpenAI 提出的让LLM输出结构化函数调用参数的能力。Tool Use 是更广义的概念，包含 Function Calling。

在本项目中，Monitor Agent 通过 Function Calling 调用 web_scraper、search_tool 等工具：
```python
tools = [
    {"type": "function", "function": {"name": "fetch_page", "parameters": {...}}},
    {"type": "function", "function": {"name": "web_search", "parameters": {...}}},
]
```

### 5. Agent 的 Memory 分为哪几种？如何实现？

**答：**
| 类型 | 存储位置 | 生命周期 | 本项目实现 |
|------|---------|---------|----------|
| 工作记忆 | LLM上下文 | 单次对话 | LangGraph State |
| 短期记忆 | Redis | 会话级 | Redis缓存 |
| 长期记忆 | 向量DB/ES | 永久 | Elasticsearch |
| 外部知识 | RAG检索 | 按需 | 搜索工具 |

### 6. 什么是 Hallucination（幻觉）？Agent系统如何防控？

**答：** 幻觉是指LLM生成看似合理但事实错误的内容。

本项目采用多层防控：
1. **Grounding**：所有分析基于爬取的真实数据，而非LLM想象
2. **Reflexion 机制**：质量评分 < 7分自动重新分析
3. **Source Citation**：要求LLM在分析中引用数据来源
4. **置信度标注**：ResearchInsight 模型包含 confidence 字段（0-1）
5. **Human-in-the-loop**：关键决策支持人工审核

### 7. 什么是 Prompt Engineering？在本项目中如何应用？

**答：** 每个Agent都有精心设计的System Prompt，遵循以下原则：
1. **角色定义**：明确Agent的专业角色（如"你是竞品情报监控Agent"）
2. **任务约束**：限定输出格式为JSON，避免自由发挥
3. **输出规范**：定义JSON schema，包括字段名、类型、取值范围
4. **Few-shot 示例**：提供输入输出样例（在更复杂的prompt中使用）

### 8. 什么是 Embedding？在竞品情报系统中有什么用？

**答：** Embedding 是将文本转换为高维向量的技术，用于语义相似度计算。

在本项目中的应用：
- **变化检测**：计算新旧网页内容的语义相似度，判断是否有实质变化
- **知识检索**：将历史竞品分析存入向量库，支持语义搜索
- **去重**：识别重复的竞品动态，避免多次告警

### 9. Token 是什么？如何优化 Token 使用？

**答：** Token 是LLM处理文本的基本单位（约3/4个英文单词 或 1-2个中文字）。

优化策略：
1. **内容截断**：网页内容截取前6000字符，避免超出上下文窗口
2. **结构化输出**：要求JSON格式，比自然语言更节省token
3. **分层处理**：Monitor只做变化检测，Research才做深度分析
4. **缓存**：相同查询结果缓存在Redis中，避免重复调用

### 10. 什么是 RAG（检索增强生成）？和本项目的关系？

**答：** RAG = Retrieval Augmented Generation，先检索相关文档，再让LLM基于检索结果生成回答。

本项目中 Research Agent 就是一个典型的 RAG 流程：
1. **Retrieve**：通过 web_search、news_search 检索竞品相关信息
2. **Augment**：将检索结果作为上下文注入prompt
3. **Generate**：LLM 基于真实数据生成分析报告

---

## 二、多Agent架构（10题）

### 11. 单Agent vs 多Agent 的区别？什么时候用多Agent？

**答：**
| 维度 | 单Agent | 多Agent |
|------|---------|---------|
| 复杂度 | 低 | 高 |
| 适用场景 | 单一任务 | 复杂工作流 |
| 专业性 | 通才 | 各有专长 |
| 可维护性 | prompt越来越长 | 各自独立维护 |
| 扩展性 | 差 | 好（新增Agent即可）|

使用多Agent的信号：
- 任务需要不同的专业能力（监控 vs 分析 vs 生成）
- 需要并行处理（Alert和Research同时运行）
- 单个prompt超出上下文窗口
- 需要独立的错误处理和重试策略

### 12. 多Agent的协作模式有哪些？

**答：** 主要有四种：
1. **Pipeline（流水线）**：A→B→C→D，顺序执行。本项目主架构
2. **Boss-Worker（老板-工人）**：一个管理Agent分发任务给多个执行Agent
3. **Joint Discussion（联合讨论）**：多个Agent围绕一个话题讨论，达成共识
4. **Event-Driven（事件驱动）**：基于事件触发Agent执行，本项目的Alert Agent

本项目采用 **Pipeline + Event-Driven** 混合模式：
- 主流程是 Pipeline：Monitor → Research → Compare → Battlecard
- Alert Agent 是事件驱动：检测到高危变化即时触发

### 13. 为什么本项目设计5个Agent？职责如何划分？

**答：** 遵循**单一职责原则（SRP）**：

| Agent | 职责 | 输入 | 输出 |
|-------|------|------|------|
| Monitor | 7×24监控竞品网站变化 | URL列表 | CompetitorChange[] |
| Research | 深度分析竞品动态 | 变化列表 | ResearchInsight[] |
| Compare | 多维度对比矩阵 | 研究结果 | ComparisonMatrix |
| Battlecard | 生成销售战术卡 | 对比矩阵 | Battlecard |
| Alert | 重大变化即时推送 | 变化列表 | Alert[] |

划分依据：
1. **技能不同**：Monitor需要爬虫能力，Research需要分析能力
2. **频率不同**：Monitor定时执行，Alert实时触发
3. **独立演进**：每个Agent可独立升级prompt和工具
4. **故障隔离**：一个Agent失败不影响其他Agent

### 14. 什么是 LangGraph？为什么选它而不是 CrewAI / AutoGen？

**答：**

| 框架 | 核心概念 | 优势 | 劣势 |
|------|---------|------|------|
| **LangGraph** | 图状态机 | 显式控制流、持久化、企业审计 | 学习曲线较陡 |
| CrewAI | 角色化团队 | 简单直觉、3-8 Agent适合 | 控制流不够精细 |
| AutoGen | 对话消息流 | 快速原型、灵活 | 生产化困难 |

选择 LangGraph 的原因：
1. **图结构**：Pipeline + 条件分支（Reflexion）天然适合图建模
2. **状态持久化**：支持 PostgreSQL/Redis 做 checkpoint
3. **企业级**：Klarna、Uber、LinkedIn 在生产环境使用
4. **可观测性**：LangSmith 集成，全链路追踪

### 15. LangGraph 的 StateGraph 是什么？如何工作？

**答：** StateGraph 是 LangGraph 的核心抽象：

```python
graph = StateGraph(PipelineState)   # 定义状态类型
graph.add_node("monitor", fn)       # 添加节点（Agent）
graph.add_edge("monitor", "research")  # 添加边（流转）
graph.add_conditional_edges(...)     # 条件分支
graph.compile()                      # 编译为可执行图
```

工作流程：
1. 初始化 State（包含competitor、空列表等）
2. 从 entry_point 开始执行
3. 每个节点接收 State、返回更新的字段
4. 通过 reducer（如 `_merge_lists`）合并更新
5. 根据边定义决定下一个节点
6. 到达 END 节点或满足终止条件时结束

### 16. 什么是 Reflexion 机制？在本项目中如何实现？

**答：** Reflexion 是让Agent自我评估并改进输出的机制，类似人类的"反思"。

本项目实现：
```
Battlecard → Quality Check（打分1-10）
                 ↓
    score < 7 → 回到 Research 重新分析
    score >= 7 → 结束，输出最终结果
```

关键设计：
- **质量评估器**：独立的LLM调用，评估完整性、准确性、可操作性
- **最大重试次数**：`MAX_REFLEXION_RETRIES=3`，防止无限循环
- **递增改进**：每次重试，Research Agent 可以看到之前的分析结果

### 17. Agent 之间如何传递数据？

**答：** 三种方式，对应三种语言实现：

| 方式 | 实现 | 适用场景 |
|------|------|---------|
| 共享状态 | LangGraph PipelineState（Python版） | 单进程Pipeline |
| 参数传递 | Java PipelineState 对象（Java版） | 单JVM内 |
| 消息队列 | Kafka topics（Go版） | 分布式、跨服务 |

Python版使用 LangGraph 的共享状态模式，通过 TypedDict + Reducer 实现：
```python
class PipelineState(TypedDict, total=False):
    changes_detected: Annotated[list, _merge_lists]  # 自动追加
    research_results: Annotated[list, _merge_lists]
    comparison_matrix: dict  # 直接覆盖
```

### 18. 事件驱动和Pipeline如何结合？

**答：** 
- **Pipeline**：确定性的顺序流（Monitor→Research→Compare→Battlecard）
- **事件驱动**：不确定性的触发（检测到高危变化→Alert立即推送）

结合方式：
1. Monitor 完成后，**同时触发**两条路径：
   - Pipeline路径：→ Research → Compare → Battlecard
   - 事件路径：→ Alert（独立执行，不阻塞Pipeline）
2. LangGraph 通过 `add_edge("monitor", "alert")` 和 `add_edge("monitor", "research")` 实现扇出

### 19. 如何保证Agent系统的可扩展性？新增一个Agent需要改什么？

**答：** 以新增 "Trend Agent"（趋势预测Agent）为例：

1. **创建Agent类**：`trend_agent.py`，实现 `__call__` 方法
2. **定义数据模型**：在 `schemas.py` 中添加 `TrendPrediction`
3. **注册到图中**：
   ```python
   graph.add_node("trend", trend_agent)
   graph.add_edge("compare", "trend")  # 插入到Pipeline中
   graph.add_edge("trend", "battlecard")
   ```
4. **无需修改其他Agent**：每个Agent只关心自己的输入输出

这就是图状态机的优势：通过添加节点和边来扩展，无需修改已有逻辑。

### 20. 多Agent系统中最常见的失败模式有哪些？

**答：**
1. **无限循环**：Agent A 调用 Agent B，B 又调用 A → 设置 max_retries
2. **上下文溢出**：累积太多历史数据超出token限制 → 内容截断 + 摘要
3. **目标漂移**：Agent 偏离原始任务 → 严格的System Prompt约束
4. **工具调用失败**：网站不可达/API限流 → tenacity重试 + 降级策略
5. **数据不一致**：并行Agent修改同一状态 → Reducer机制保证一致性
6. **级联失败**：一个Agent失败导致整个Pipeline崩溃 → 错误隔离 + fallback

---

## 三、技术栈深度（15题）

### 21. Kafka 在多Agent系统中起什么作用？能不能用Redis替代？

**答：** Kafka 的作用：
1. **Agent解耦**：Agent之间通过topic通信，不直接依赖
2. **事件持久化**：所有竞品变化事件持久化到Kafka，可回溯
3. **流量削峰**：Monitor批量产生变化事件，Research按能力消费
4. **审计日志**：所有Agent的输入输出都有完整记录

能否用Redis替代？
| 维度 | Kafka | Redis Streams |
|------|-------|--------------|
| 持久化 | 磁盘，长期保存 | 内存为主，受限 |
| 吞吐量 | 百万级/秒 | 十万级/秒 |
| 消费模型 | Consumer Group，可重放 | 类似，但生态较弱 |
| 适用场景 | 生产环境 | 原型/小规模 |

**结论**：小规模可以用Redis Streams替代，但生产环境推荐Kafka。

### 22. Elasticsearch 在竞品数据检索中如何使用？

**答：**
- **索引结构**：每个竞品一个index（如 `ci-competitora`）
- **文档类型**：changes、research、battlecards
- **查询场景**：
  1. 全文搜索：搜索包含特定关键词的竞品动态
  2. 时间范围：查询最近30天的定价变化
  3. 聚合分析：统计各竞品的变化频率
  4. 语义搜索：结合 embedding 做相似度查询

### 23. Redis 在本系统中的具体用途？

**答：**
1. **页面Hash缓存**：存储上次爬取的content_hash，用于变化检测
2. **LLM结果缓存**：相同查询缓存结果，节省API费用
3. **限流控制**：防止爬虫频率过高触发反爬
4. **会话状态**：Pipeline 运行中间状态的临时存储

### 24. SSE（Server-Sent Events）和 WebSocket 的区别？为什么选SSE？

**答：**
| 维度 | SSE | WebSocket |
|------|-----|-----------|
| 方向 | 服务端→客户端（单向） | 双向 |
| 协议 | HTTP | ws:// |
| 复杂度 | 低 | 高 |
| 自动重连 | 内建 | 需自己实现 |
| 适用场景 | 实时推送 | 聊天/协作 |

选SSE的原因：Pipeline状态推送是单向的（服务端→前端），不需要双向通信。SSE更简单且内建重连。

### 25. FastAPI 的异步特性在本项目中如何利用？

**答：**
1. **异步爬虫**：`httpx.AsyncClient` 并行爬取多个URL
2. **异步LLM调用**：`llm.ainvoke()` 不阻塞线程
3. **SSE流式输出**：`async for event in pipeline.astream()` 逐步推送
4. **并发请求**：多个用户可以同时发起分析请求

### 26. Docker Compose 在本项目中如何编排多个服务？

**答：** docker-compose.yml 定义了5个服务：
- `api`：FastAPI应用（依赖其他所有服务）
- `kafka` + `zookeeper`：消息队列
- `elasticsearch`：搜索引擎
- `redis`：缓存

关键设计：
- `depends_on` + `healthcheck` 确保依赖服务就绪后才启动API
- `env_file: .env` 统一管理配置
- `restart: unless-stopped` 自动故障恢复

### 27. 如何实现竞品网页的变化检测？

**答：** 三层检测策略：

1. **Hash对比**（快速）：
   ```python
   new_hash = sha256(page_content)
   if new_hash == cached_hash: return "no change"
   ```

2. **关键区域提取**（精确）：
   ```python
   pricing = extract_pricing(html)  # CSS选择器提取定价
   jobs = extract_job_listings(html)  # 提取招聘信息
   ```

3. **LLM语义分析**（深度）：将新旧内容交给LLM，判断是否有有意义的变化

### 28. 如果竞品网站反爬怎么办？

**答：** 多层应对策略：
1. **随机UA + 请求间隔**：模拟正常用户行为
2. **代理池**：轮换IP避免被封
3. **Headless浏览器**：Playwright 处理JS渲染页面
4. **官方API优先**：优先使用RSS、API而非爬虫
5. **新闻搜索替代**：用搜索引擎获取公开信息
6. **降级策略**：爬取失败时使用搜索结果作为替代数据源

### 29. Scrapy vs BeautifulSoup vs Playwright 的选型考量？

**答：**
| 工具 | 适用场景 | 性能 | JS支持 |
|------|---------|------|--------|
| Scrapy | 大规模结构化爬取 | 高 | 需插件 |
| BeautifulSoup | 简单HTML解析 | 中 | 不支持 |
| Playwright | JS动态渲染页面 | 低 | 完整支持 |

本项目选择 `httpx + BeautifulSoup` 作为默认方案（轻量），Scrapy 用于大规模定时爬取任务。

### 30. LangChain4j 和 Python LangChain 的区别？

**答：** LangChain4j 是 Java 生态的 LangChain 实现：
- 支持 Java 21 特性（虚拟线程、Record、Sealed Class）
- 类型安全的结构化输出
- 原生Spring Boot集成
- LLM Provider 抽象层统一了 OpenAI/Anthropic/Ollama 等

Java版使用 LangChain4j 而非直接调用 OpenAI API，是因为它提供了更好的抽象和未来的 Provider 切换能力。

### 31. Go 版为什么选 Gin 而不是 Echo / Fiber？

**答：** Gin 是 Go 生态最成熟的 HTTP 框架，选择原因：
- Stars 最多（80k+），社区最活跃
- 中间件生态丰富
- 性能优秀（基于 httprouter）
- 文档完善，面试官大概率熟悉

### 32. 三种语言版本的技术栈对比？

**答：**
| 组件 | Python | Java | Go |
|------|--------|------|----|
| 框架 | LangGraph + FastAPI | LangChain4j + Spring Boot | 自研 + Gin |
| LLM客户端 | langchain-openai | langchain4j-open-ai | go-openai |
| 爬虫 | httpx + BeautifulSoup | Jsoup | goquery |
| API | FastAPI（异步） | Spring MVC | Gin |
| 实时推送 | SSE | WebSocket | SSE |
| 消息队列 | confluent-kafka | spring-kafka | confluent-kafka-go |
| 序列化 | Pydantic | Jackson + Lombok | encoding/json |

### 33. 如何保证LLM输出的JSON格式正确？

**答：** 多层保障：
1. **Prompt约束**：明确要求"Return ONLY a JSON..."
2. **格式清洗**：移除markdown代码块标记（```json ... ```）
3. **Schema验证**：用Pydantic/Jackson验证返回的JSON结构
4. **Fallback**：解析失败时返回默认值或原始文本
5. **结构化输出**（高级）：OpenAI的 `response_format={"type": "json_object"}`

### 34. 项目的可观测性如何实现？

**答：**
| 层面 | 工具 | 监控内容 |
|------|------|---------|
| 应用日志 | structlog | Agent执行日志、错误追踪 |
| LLM追踪 | LangSmith | prompt/response/token/耗时 |
| 指标监控 | Prometheus + Grafana | API延迟、Agent成功率 |
| 分布式追踪 | OpenTelemetry | 跨Agent调用链路 |

关键指标：
- Pipeline 端到端耗时
- 每个Agent的LLM调用延迟
- Reflexion重试率
- 爬虫成功率
- 告警发送成功率

### 35. 如何实现优雅降级？

**答：**
1. **LLM不可用**：返回缓存的上次分析结果
2. **爬虫失败**：切换到搜索引擎获取公开信息
3. **Kafka宕机**：切换到内存队列（有限容量）
4. **ES不可用**：跳过历史数据检索，只用实时数据
5. **通知渠道失败**：自动切换备用渠道（Slack→DingTalk→Email）

---

## 四、系统设计（10题）

### 36. 如何设计一个生产级Agent系统？你的架构分几层？

**答：** 四层架构：

```
┌─────────────────────────────┐
│  API Layer（FastAPI/Spring）  │  ← 接入层
├─────────────────────────────┤
│  Orchestration（LangGraph）   │  ← 编排层
├─────────────────────────────┤
│  Agent Layer（5个Agent）      │  ← 智能层
├─────────────────────────────┤
│  Tool Layer（爬虫/搜索/通知）  │  ← 工具层
├─────────────────────────────┤
│  Infrastructure（Kafka/ES/Redis） │ ← 基础设施层
└─────────────────────────────┘
```

### 37. 如果要支持100个竞品的监控，架构需要怎么调整？

**答：**
1. **水平扩展Monitor**：多实例消费Kafka的"competitor-tasks" topic
2. **分级监控**：重点竞品每小时、次要竞品每天
3. **分布式爬虫**：Scrapy + Redis分布式调度
4. **异步Pipeline**：Monitor产出投入Kafka，Research异步消费
5. **结果聚合**：ES做全局搜索，Dashboard展示

### 38. Pipeline 的幂等性如何保证？

**答：**
- 每次Pipeline运行有唯一 `run_id`
- Kafka 消息带幂等key（competitor + timestamp）
- ES 文档使用 `run_id` 作为 doc_id，重复写入会覆盖
- Redis 缓存设置TTL，过期自动清理

### 39. 如何处理敏感信息（如竞品的非公开数据）？

**答：** 本系统**只采集公开信息**：
- 官方网站（定价页、招聘页、博客）
- 公开新闻报道
- 公开的财务报告
- 公开的开源仓库

**严格禁止**：
- 绕过登录/付费墙
- 获取竞品内部文件
- 社工/钓鱼获取信息

### 40. 如何估算系统的 LLM API 成本？

**答：** 假设监控10个竞品，每天1次全流程分析：

| Agent | 每次Token数 | 每天调用次数 | 每天Token |
|-------|-----------|------------|----------|
| Monitor | ~3000 | 40（10竞品×4 URL） | 120,000 |
| Research | ~5000 | 10 | 50,000 |
| Compare | ~3000 | 10 | 30,000 |
| Battlecard | ~3000 | 10 | 30,000 |
| Quality | ~2000 | 15（含重试） | 30,000 |

**每天约 260K tokens ≈ $1.3/天（GPT-4o）**
**每月约 $40**

优化方案：非关键Agent使用更便宜的模型（GPT-4o-mini）。

### 41. 系统的 SLA 如何设计？

**答：**
| 指标 | 目标 |
|------|------|
| 变化检测延迟 | < 1小时 |
| 全流程分析延迟 | < 5分钟/竞品 |
| 告警推送延迟 | < 1分钟 |
| 系统可用性 | 99.5% |
| 分析质量评分 | ≥ 7.0/10 |

### 42. 如何做 A/B 测试不同的 Agent prompt？

**答：**
1. Prompt版本化管理（存储在配置中心）
2. 流量分割：50%走新prompt，50%走旧prompt
3. 用Quality Check打分对比两个版本的输出质量
4. LangSmith Evaluator 做自动化评估

### 43. 你遇到的最大技术挑战是什么？

**答：** **LLM输出的不确定性。**

同样的输入，LLM每次输出可能不同，这给下游Agent的解析和处理带来困难。

解决方案：
1. 降低 temperature（0.3）减少随机性
2. JSON mode 强制格式输出
3. 多层容错解析（正则 → JSON → fallback）
4. Reflexion 机制保证最终输出质量

### 44. 安全性设计考虑了哪些方面？

**答：**
1. **API Key 安全**：所有密钥通过环境变量注入，不硬编码
2. **输入校验**：所有API输入经过Pydantic验证
3. **Prompt注入防护**：用户输入不直接拼接到System Prompt
4. **速率限制**：API层限流防止滥用
5. **数据隔离**：多租户场景下按tenant_id隔离数据

### 45. 如果让你重新设计，会有什么改进？

**答：**
1. 引入 **A2A协议** 做Agent间标准化通信
2. 使用 **MCP协议** 统一工具层接口
3. 添加 **前端Dashboard** 实时展示竞品动态
4. 引入 **向量数据库（Pinecone/Milvus）** 做语义搜索
5. 使用 **LangSmith** 做全链路可观测性
6. 增加 **Trend Agent** 做趋势预测

---

## 五、工程实践（5+题）

### 46. 项目的测试策略？

**答：**
| 层次 | 测试类型 | 工具 | 覆盖内容 |
|------|---------|------|---------|
| 单元测试 | Pydantic模型验证 | pytest | 数据模型字段约束 |
| 集成测试 | Pipeline端到端 | pytest + mock | 全流程走通 |
| Agent测试 | LLM输出质量 | LangSmith Eval | 分析结果准确性 |
| 性能测试 | API负载 | locust | 并发处理能力 |

### 47. CI/CD 流程设计？

**答：**
```
Push → GitHub Actions → Lint + Test → Docker Build → Deploy to K8s
```
- Pre-commit: black + ruff + mypy
- CI: pytest + coverage（≥80%）
- CD: Docker build → push to ECR → K8s rolling update

### 48. 日志规范？

**答：** 使用 structlog 结构化日志：
```python
logger.info("Agent completed", agent="monitor", competitor="Acme",
            changes_count=3, duration_ms=1200)
```
- 每条日志包含：agent_name, competitor, run_id, timestamp
- 错误日志包含完整stack trace
- JSON格式输出，便于ELK采集

### 49. 配置管理方案？

**答：**
- 开发环境：`.env` 文件
- 测试环境：Docker环境变量
- 生产环境：Kubernetes ConfigMap + Secret
- 敏感配置（API Key）：Vault 或 K8s Secret

### 50. 代码规范？

**答：**
- Python: black + ruff + mypy + isort
- Java: Google Java Style + Checkstyle
- Go: gofmt + golangci-lint
- Commit: Conventional Commits（feat/fix/docs）
- PR: 至少1人Review
