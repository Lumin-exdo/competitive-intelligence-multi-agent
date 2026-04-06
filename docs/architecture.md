# 架构设计文档

## 1. 系统总览

多Agent竞品情报系统是一个基于LLM的自动化竞品分析平台，核心目标是将人工竞品调研的工作从每周16小时缩短到30分钟。

## 2. 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Layer                                 │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────────────┐     │
│  │  /health  │  │  /analyze    │  │  /analyze/stream (SSE) │     │
│  └──────────┘  └──────────────┘  └────────────────────────┘     │
├─────────────────────────────────────────────────────────────────┤
│                    Orchestration Layer                            │
│                                                                   │
│  ┌────────────────── LangGraph StateGraph ──────────────────┐   │
│  │                                                            │   │
│  │  [START] → [Monitor] ─┬→ [Alert] → [END]                │   │
│  │                        │                                   │   │
│  │                        └→ [Research] → [Compare]          │   │
│  │                              ↑           │                 │   │
│  │                              │           ▼                 │   │
│  │                              │      [Battlecard]          │   │
│  │                              │           │                 │   │
│  │                              │           ▼                 │   │
│  │                              │    [Quality Check]         │   │
│  │                              │      ┌────┴────┐           │   │
│  │                              │   <7 │         │ >=7       │   │
│  │                              └──────┘      [END]          │   │
│  └────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                       Agent Layer                                │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌───────┐ │
│  │ Monitor  │ │ Research │ │ Compare │ │Battlecard│ │ Alert │ │
│  │  Agent   │ │  Agent   │ │  Agent  │ │  Agent   │ │ Agent │ │
│  └────┬─────┘ └────┬─────┘ └────┬────┘ └────┬─────┘ └───┬───┘ │
│       │             │            │            │           │      │
├───────┴─────────────┴────────────┴────────────┴───────────┴─────┤
│                       Tool Layer                                 │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐                 │
│  │Web Scraper│  │Search API│  │ Notification  │                 │
│  │(httpx+BS4)│  │(SerpAPI) │  │(Slack/DingTalk)│                │
│  └──────────┘  └──────────┘  └───────────────┘                 │
├─────────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                            │
│  ┌─────────┐  ┌──────────────┐  ┌───────┐  ┌──────┐           │
│  │  Kafka   │  │Elasticsearch │  │ Redis │  │ LLM  │           │
│  │(事件流) │  │(历史数据存储) │  │(缓存) │  │(API) │           │
│  └─────────┘  └──────────────┘  └───────┘  └──────┘           │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 数据流

### 3.1 主流程（Pipeline）

```
1. 用户发起 POST /analyze {"competitor": "Acme"}
2. Monitor Agent:
   - 爬取竞品官网/定价页/招聘页/博客
   - SHA-256 Hash对比检测变化
   - LLM分析变化的业务含义
   - 输出: CompetitorChange[]

3. Research Agent:
   - 搜索竞品相关新闻、财报、专利
   - LLM深度分析各维度
   - 输出: ResearchInsight[]

4. Compare Agent:
   - 基于研究结果生成8维度对比矩阵
   - 输出: ComparisonMatrix

5. Battlecard Agent:
   - 生成销售战术卡（优劣势/异议处理/电梯演讲）
   - 输出: Battlecard

6. Quality Check:
   - 独立LLM评估战术卡质量（1-10分）
   - >=7分: 输出最终结果
   - <7分: 回到Research重新分析（最多3次）

7. 返回完整结果给用户
```

### 3.2 告警流程（Event-Driven）

```
1. Monitor Agent检测到HIGH/CRITICAL级别变化
2. Alert Agent立即触发（与Pipeline并行）
3. 调用Slack Webhook / 钉钉Webhook推送通知
4. 记录发送状态
```

## 4. Agent 详细设计

### 4.1 Monitor Agent

| 属性 | 说明 |
|------|------|
| 职责 | 7×24监控竞品网站变化 |
| 输入 | competitor name, URL列表 |
| 输出 | CompetitorChange[] |
| 工具 | web_scraper (httpx + BeautifulSoup) |
| LLM用途 | 分析页面变化的业务含义和严重程度 |
| 关键设计 | 三层检测（Hash→结构化提取→LLM语义分析） |

### 4.2 Research Agent

| 属性 | 说明 |
|------|------|
| 职责 | 深度分析竞品动态 |
| 输入 | competitor name, CompetitorChange[] |
| 输出 | ResearchInsight[] |
| 工具 | web_search, news_search |
| LLM用途 | 综合多源信息生成结构化分析 |
| 关键设计 | RAG模式（先检索再分析） |

### 4.3 Compare Agent

| 属性 | 说明 |
|------|------|
| 职责 | 多维度竞品对比 |
| 输入 | competitor name, ResearchInsight[] |
| 输出 | ComparisonMatrix（8个维度，0-10分） |
| 工具 | 无（纯LLM推理） |
| 关键设计 | 固定8维度评分，保证一致性 |

### 4.4 Battlecard Agent

| 属性 | 说明 |
|------|------|
| 职责 | 生成销售战术卡 |
| 输入 | ComparisonMatrix, ResearchInsight[] |
| 输出 | Battlecard（优劣势/异议处理/电梯演讲） |
| 工具 | 无（纯LLM推理） |
| 关键设计 | 面向销售人员的语言风格，简洁可操作 |

### 4.5 Alert Agent

| 属性 | 说明 |
|------|------|
| 职责 | 重大变化即时推送 |
| 输入 | CompetitorChange[] |
| 输出 | Alert[]（发送记录） |
| 工具 | notification (Slack/DingTalk/Email) |
| 关键设计 | 只推送HIGH/CRITICAL，避免告警疲劳 |

## 5. 技术选型理由

| 技术 | 选型理由 | 替代方案 |
|------|---------|---------|
| LangGraph | 图状态机、条件分支、checkpoint持久化 | CrewAI、AutoGen |
| FastAPI | 异步、自动文档、SSE支持 | Flask、Django |
| Kafka | 事件持久化、解耦、削峰 | Redis Streams |
| Elasticsearch | 全文检索、聚合分析 | PostgreSQL |
| Redis | 高速缓存、Hash存储 | Memcached |
| Pydantic | 类型安全、自动校验 | dataclasses |

## 6. 部署架构

### 开发环境
```
Docker Compose 一键启动
- API容器
- Kafka + Zookeeper
- Elasticsearch
- Redis
```

### 生产环境
```
Kubernetes
- API: Deployment (3 replicas) + HPA
- Kafka: StatefulSet (3 brokers)
- ES: StatefulSet (3 nodes)
- Redis: Sentinel (1 master + 2 replicas)
- Ingress: Nginx + TLS
```
