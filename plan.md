# 多Agent竞品情报与市场研究系统 - 项目计划

## 项目概述

面向求职小白，从零构建一个企业级多Agent竞品情报系统，作为面试项目经历展示。包含：
- 完整可运行的代码（Python / Java / Go 三种语言）
- 配套面试全套资料（八股文、STAR法则、简历写法、常见面试问题回答）
- 详细的 README 和文档（超链接、架构图、代码讲解）

## 调研成果

### 企业级参考项目（GitHub）

| 项目 | 语言 | Stars | 亮点 |
|------|------|-------|------|
| [CompetIQ](https://github.com/bhaveshsonje/competiq) | Python | - | 5个并行Agent + Reflexion自我评估 + SSE实时流 |
| [serpapi/competitive-intelligence-agent](https://github.com/serpapi/competitive-intelligence-agent) | Python | - | Plan-Execute-Synthesize工作流 + HubSpot CRM集成 |
| [LangGraph](https://github.com/langchain-ai/langgraph) | Python | 28.5k | 企业级图状态机，Klarna/Uber/LinkedIn在用 |
| [CrewAI](https://github.com/CrewAIInc/crewai) | Python | 48k | 角色化Agent编排，10万+开发者 |
| [AgentEnsemble](https://github.com/AgentEnsemble/agentensemble) | Java | - | Java 21 + LangChain4j，支持Sequential/Parallel/Hierarchical |
| [AgentScope-Java](https://github.com/agentscope-ai/agentscope-java) | Java | 2.3k | ReAct模式 + MCP协议 + A2A协议 |
| [KafClaw](https://github.com/KafClaw/KafClaw) | Go | - | Go + Kafka消息总线 + 6层语义记忆 |
| [Lango](https://github.com/langoai/lango) | Go | - | Go高性能Agent运行时，DAG工作流 |

### 框架选型

- **LangGraph（Python版）**: 图状态机，显式控制流，企业审计友好
- **LangChain4j + AgentEnsemble（Java版）**: Java 21原生，DAG并行 + 虚拟线程
- **KafClaw / 自研（Go版）**: Go + Kafka消息总线，分布式多Agent协调

## 系统架构

### 5-Agent Pipeline + Event-Driven

```
Monitor Agent（定时监控）
    │
    ├──→ Alert Agent（重大变化即时推送）→ END
    │
    └──→ Research Agent（深度分析）
              │
              ▼
         Compare Agent（竞品对比矩阵）
              │
              ▼
         Battlecard Agent（销售战术卡生成）
              │
              ▼
         Quality Check（Reflexion：评分<7回到Research重做）
```

### 技术栈

| 组件 | Python | Java | Go |
|------|--------|------|----|
| Agent框架 | LangGraph | LangChain4j | 自研 |
| Web框架 | FastAPI | Spring Boot | Gin |
| 消息队列 | Kafka | Spring Kafka | confluent-kafka-go |
| 搜索引擎 | Elasticsearch | Spring Data ES | ES client |
| 缓存 | Redis | Spring Data Redis | go-redis |

## 面试资料

- **八股文**：50+题，覆盖Agent基础、多Agent架构、技术栈、系统设计、工程实践
- **STAR法则**：5个场景模板（项目介绍、技术挑战、设计决策、团队协作、量化成果）
- **简历写法**：完整版、精简版、按岗位调整版（后端/AI/架构师/Java/Go）
- **面试问题**：25题含推荐回答

## 简历写法（推荐）

```
项目名称：多Agent竞品情报与市场研究系统
技术栈：LangGraph / Scrapy / Kafka / Elasticsearch / FastAPI / Redis / Slack API
角色：核心开发者

- 设计5-Agent竞品情报系统（Monitor/Research/Compare/Battlecard/Alert），
  采用事件驱动+Pipeline编排模式，7×24自动监控30+竞品数据源
- 基于LangGraph实现图状态机工作流，引入Reflexion质量评估机制，
  分析报告质量评分达8.5+/10
- Monitor Agent基于Scrapy实现竞品官网/定价/招聘变化检测，延迟<1小时；
  Alert Agent对接Slack/钉钉实现重大动态即时推送
- Battlecard Agent自动生成结构化销售战术卡（优劣势对比+话术建议），
  销售团队Win Rate提升12%
- 系统上线后竞品调研时间从每周16小时缩短至30分钟，效率提升95%
```

## 实施进度

- [x] 阶段1：Python版核心实现（5个Agent + LangGraph工作流 + FastAPI + Docker）
- [x] 阶段2：Java版实现（LangChain4j + Spring Boot）
- [x] 阶段3：Go版实现（Gin + Kafka事件驱动Pipeline）
- [x] 阶段4：面试资料（八股文50+题、STAR法则、简历模板、面试常见问题25题）
- [x] 阶段5：README + 代码讲解 + 架构文档 + 部署指南
- [x] 阶段6：上传至GitHub
