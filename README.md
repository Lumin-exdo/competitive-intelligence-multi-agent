# Multi-Agent Competitive Intelligence System

# 多Agent竞品情报与市场研究系统

> 企业级 5-Agent 竞品情报自动化系统 | Python + Java + Go 三语言实现 | 配套面试全套资料

[![Python](https://img.shields.io/badge/Python-3.12-blue.svg)](python/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](java/)
[![Go](https://img.shields.io/badge/Go-1.22-00ADD8.svg)](go/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 这个项目是什么？

一个**生产级**的多Agent竞品情报系统，能够：

- **7x24自动监控**竞品官网、定价页、招聘页的变化
- **深度分析**竞品的财务、专利、技术动态
- **自动生成**多维度竞品对比矩阵和销售战术卡
- **即时推送**重大竞品动态到Slack/钉钉

同时，本项目还是一份**面试项目经历模板**，包含：
- 50+ 道八股文题目及详细答案
- STAR法则面试回答模板（5个场景）
- 简历写法模板（完整版/精简版/不同岗位版）
- 25道面试常见问题及推荐回答

---

## 目录

- [架构设计](#架构设计)
- [5个Agent详细介绍](#5个agent详细介绍)
- [三种语言实现](#三种语言实现)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [API文档](#api文档)
- [面试准备指南](#面试准备指南)
- [代码讲解导航](#代码讲解导航)
- [学习路线图](#学习路线图)
- [技术选型对比](#技术选型对比)
- [FAQ](#faq)
- [参考资料](#参考资料)

---

## 架构设计

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         API Layer                            │
│        FastAPI (Python) / Spring Boot (Java) / Gin (Go)     │
├─────────────────────────────────────────────────────────────┤
│                    Orchestration Layer                        │
│                                                               │
│  [START] → [Monitor] ─┬→ [Alert] ───────→ [END]            │
│                        │                                      │
│                        └→ [Research] → [Compare]             │
│                              ↑            │                   │
│                              │            ▼                   │
│                              │      [Battlecard]             │
│                              │            │                   │
│                              │            ▼                   │
│                              │     [Quality Check]           │
│                              │       ┌────┴────┐             │
│                              │    <7 │         │ >=7         │
│                              └───────┘      [END]            │
├─────────────────────────────────────────────────────────────┤
│                       Agent Layer                            │
│   Monitor  │  Research  │  Compare  │  Battlecard  │  Alert │
├─────────────────────────────────────────────────────────────┤
│                       Tool Layer                             │
│    Web Scraper  │  Search API  │  Notification (Slack/钉钉) │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                        │
│       Kafka  │  Elasticsearch  │  Redis  │  LLM (OpenAI)   │
└─────────────────────────────────────────────────────────────┘
```

### 编排模式：事件驱动 + Pipeline

- **Pipeline**（确定性顺序流）：Monitor → Research → Compare → Battlecard
- **Event-Driven**（即时触发）：Monitor检测到高危变化 → Alert立即推送
- **Reflexion**（质量反馈环）：Battlecard质量评分<7分 → 回到Research重做

> 详细架构文档：[docs/architecture.md](docs/architecture.md)

---

## 5个Agent详细介绍

### 1. Monitor Agent（监控Agent）

| 属性 | 说明 |
|------|------|
| **职责** | 7×24监控竞品官网、定价、招聘页变化 |
| **工具** | httpx + BeautifulSoup（Python）/ Jsoup（Java）/ goquery（Go） |
| **检测策略** | 三层：SHA-256 Hash快筛 → CSS选择器结构化提取 → LLM语义分析 |
| **输出** | `CompetitorChange[]`（变化类型、标题、摘要、严重程度） |
| **代码位置** | [Python](python/src/agents/monitor_agent.py) · [Java](java/src/main/java/com/ci/agents/MonitorAgent.java) · [Go](go/internal/agents/monitor.go) |

### 2. Research Agent（研究Agent）

| 属性 | 说明 |
|------|------|
| **职责** | 深度分析竞品财报、专利、技术博客、开源动态 |
| **模式** | RAG（检索增强生成）：先搜索多源信息，再让LLM综合分析 |
| **搜索维度** | 财务、专利、技术博客、开源贡献、战略动态 |
| **输出** | `ResearchInsight[]`（主题、摘要、关键发现、来源、置信度） |
| **代码位置** | [Python](python/src/agents/research_agent.py) · [Java](java/src/main/java/com/ci/agents/ResearchAgent.java) · [Go](go/internal/agents/research.go) |

### 3. Compare Agent（对比Agent）

| 属性 | 说明 |
|------|------|
| **职责** | 生成8维度量化对比矩阵 |
| **对比维度** | 产品功能、定价、UX、市场份额、口碑、技术、生态、支持 |
| **输出** | `ComparisonMatrix`（每个维度的双方评分0-10 + 说明） |
| **代码位置** | [Python](python/src/agents/compare_agent.py) · [Java](java/src/main/java/com/ci/agents/CompareAgent.java) · [Go](go/internal/agents/compare.go) |

### 4. Battlecard Agent（战术卡Agent）

| 属性 | 说明 |
|------|------|
| **职责** | 自动生成销售战术卡（我们 vs 竞品） |
| **输出内容** | 我方优势/劣势、竞品优势/劣势、关键差异化、异议处理话术、电梯演讲 |
| **目标用户** | 销售团队 |
| **代码位置** | [Python](python/src/agents/battlecard_agent.py) · [Java](java/src/main/java/com/ci/agents/BattlecardAgent.java) · [Go](go/internal/agents/battlecard.go) |

### 5. Alert Agent（预警Agent）

| 属性 | 说明 |
|------|------|
| **职责** | HIGH/CRITICAL级别变化即时推送 |
| **推送渠道** | Slack Webhook、钉钉Webhook、Email |
| **触发条件** | 变化严重程度为HIGH或CRITICAL |
| **代码位置** | [Python](python/src/agents/alert_agent.py) · [Java](java/src/main/java/com/ci/agents/AlertAgent.java) · [Go](go/internal/agents/alert.go) |

---

## 三种语言实现

本项目提供 Python、Java、Go 三种完整实现，核心架构一致，适合不同技术栈的开发者：

| 维度 | Python | Java | Go |
|------|--------|------|----|
| **Agent框架** | LangGraph StateGraph | LangChain4j + Spring | 自研轻量框架 |
| **Web框架** | FastAPI（异步） | Spring Boot 3.3 | Gin |
| **LLM客户端** | langchain-openai | langchain4j-open-ai | go-openai |
| **爬虫** | httpx + BeautifulSoup | Jsoup | goquery |
| **实时推送** | SSE | WebSocket | SSE |
| **序列化** | Pydantic | Lombok + Jackson | struct tag |
| **消息队列** | confluent-kafka | spring-kafka | confluent-kafka-go |
| **部署形态** | Docker容器 | JAR + JVM | 单二进制文件 |
| **适用场景** | 功能最全 | 企业Java技术栈 | 高性能/资源受限 |

### 为什么提供三种语言？

- **Python版**（推荐首选）：功能最完整，LangGraph生态最成熟
- **Java版**：适合面试Java岗位，展示Spring Boot + LangChain4j能力
- **Go版**：适合面试Go岗位，展示高性能微服务能力

---

## 快速开始

### 方式一：Docker 一键启动（推荐）

```bash
# 1. 克隆仓库
git clone https://github.com/your-username/competitive-intelligence-multi-agent.git
cd competitive-intelligence-multi-agent/python

# 2. 配置
cp .env.example .env
# 编辑 .env，填入 OPENAI_API_KEY

# 3. 启动
docker-compose up -d

# 4. 测试
curl http://localhost:8000/health
curl -X POST http://localhost:8000/analyze \
  -H "Content-Type: application/json" \
  -d '{"competitor": "Stripe"}'
```

### 方式二：本地运行（Python）

```bash
cd python
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env  # 编辑填入API Key
uvicorn src.api.server:app --reload --port 8000
```

### 方式三：Java版

```bash
cd java
mvn clean package -DskipTests
OPENAI_API_KEY=sk-your-key java -jar target/competitive-intelligence-1.0.0.jar
```

### 方式四：Go版

```bash
cd go
go build -o ci-agent ./cmd/server
OPENAI_API_KEY=sk-your-key ./ci-agent
```

> 详细部署指南：[docs/deployment.md](docs/deployment.md)

---

## 项目结构

```
competitive-intelligence-multi-agent/
│
├── README.md                          ← 你正在看的这个文件
├── plan.md                            ← 项目计划文档
│
├── python/                            ← Python 版（LangGraph + FastAPI）
│   ├── src/
│   │   ├── agents/                    ← 5个Agent实现
│   │   │   ├── monitor_agent.py
│   │   │   ├── research_agent.py
│   │   │   ├── compare_agent.py
│   │   │   ├── battlecard_agent.py
│   │   │   └── alert_agent.py
│   │   ├── graph/workflow.py          ← LangGraph工作流（核心！）
│   │   ├── tools/                     ← 工具层（爬虫/搜索/通知）
│   │   ├── models/schemas.py          ← 数据模型
│   │   ├── api/server.py              ← FastAPI服务
│   │   └── config.py                  ← 配置管理
│   ├── tests/                         ← 测试
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
│
├── java/                              ← Java 版（Spring Boot + LangChain4j）
│   ├── src/main/java/com/ci/
│   │   ├── agents/                    ← 5个Agent
│   │   ├── workflow/CIPipeline.java   ← Pipeline编排
│   │   ├── tools/                     ← 工具层
│   │   ├── model/                     ← 数据模型
│   │   └── api/CIController.java      ← REST API
│   └── pom.xml
│
├── go/                                ← Go 版（Gin + Kafka）
│   ├── internal/
│   │   ├── agents/                    ← 5个Agent
│   │   ├── pipeline/pipeline.go       ← Pipeline编排
│   │   ├── tools/                     ← 工具层
│   │   ├── models/                    ← 数据模型
│   │   └── api/server.go              ← Gin HTTP服务
│   ├── cmd/server/main.go             ← 入口
│   └── go.mod
│
└── docs/                              ← 文档
    ├── architecture.md                ← 架构设计文档
    ├── deployment.md                  ← 部署指南
    ├── interview/                     ← 面试资料
    │   ├── eight-part-essay.md        ← 八股文50+题
    │   ├── star-method.md             ← STAR法则回答模板
    │   ├── resume-template.md         ← 简历写法模板
    │   └── common-questions.md        ← 面试常见问题25题
    └── code-walkthrough/              ← 代码逐行讲解
        ├── python-walkthrough.md
        ├── java-walkthrough.md
        └── go-walkthrough.md
```

---

## API文档

启动Python版后访问 http://localhost:8000/docs 查看自动生成的Swagger文档。

### 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/analyze` | 同步运行完整Pipeline |
| POST | `/analyze/stream` | SSE流式输出Pipeline进度 |
| GET | `/competitors` | 获取预配置竞品列表 |

### 请求示例

```bash
# 同步分析
curl -X POST http://localhost:8000/analyze \
  -H "Content-Type: application/json" \
  -d '{"competitor": "Stripe", "urls": ["https://stripe.com/pricing"]}'

# SSE流式分析
curl -N -X POST http://localhost:8000/analyze/stream \
  -H "Content-Type: application/json" \
  -d '{"competitor": "Stripe"}'
```

---

## 面试准备指南

本项目配套完整的面试准备资料，帮你从零到拿到Offer。

### 1. 八股文题库（50+ 题含详细答案）

覆盖 Agent 基础、多Agent架构、技术栈深度、系统设计、工程实践五大模块。

> [docs/interview/eight-part-essay.md](docs/interview/eight-part-essay.md)

**精选题目预览：**
- 什么是AI Agent？和传统聊天机器人的区别？
- ReAct vs CoT vs ToT 的区别？
- 为什么设计5个Agent？能合并吗？
- 为什么选LangGraph而不是CrewAI？
- Kafka在系统中起什么作用？能用Redis替代吗？
- Reflexion机制怎么实现？
- 如何保证LLM输出的JSON格式正确？
- 如果支持100个竞品，架构怎么调整？

### 2. STAR法则回答模板（5个场景）

用结构化的STAR法则回答行为面试问题。

> [docs/interview/star-method.md](docs/interview/star-method.md)

**包含场景：**
- 项目整体介绍
- 最大技术挑战
- 系统设计决策
- 团队协作
- 量化成果

### 3. 简历写法模板

> [docs/interview/resume-template.md](docs/interview/resume-template.md)

**包含版本：**
- 完整版（核心项目）
- 精简版（多项目列举）
- 后端开发版、AI工程师版、架构师版
- Java开发版、Go开发版

### 4. 面试常见问题（25题含推荐回答）

> [docs/interview/common-questions.md](docs/interview/common-questions.md)

覆盖架构设计、技术深度、编码能力、个人成长四个维度。

---

## 代码讲解导航

面向小白的逐行代码讲解，建议按以下顺序阅读：

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [Python版讲解](docs/code-walkthrough/python-walkthrough.md) | 主版本，最详细（数据模型→配置→工具→Agent→工作流→API） |
| 2 | [Java版讲解](docs/code-walkthrough/java-walkthrough.md) | Spring Boot + LangChain4j 特色讲解 |
| 3 | [Go版讲解](docs/code-walkthrough/go-walkthrough.md) | Gin + goroutine 并发模型讲解 |

**建议阅读顺序**：先看 Python 版理解核心逻辑，再看 Java/Go 版了解不同语言的实现差异。

---

## 学习路线图

从零到面试的完整路径：

### 阶段一：理解基础概念（1-2天）

1. 阅读[八股文题库](docs/interview/eight-part-essay.md)的前10题（Agent基础）
2. 理解 ReAct、RAG、Prompt Engineering 等核心概念
3. 了解 LangGraph 的 StateGraph 工作原理

### 阶段二：通读代码（2-3天）

1. 按照[Python代码讲解](docs/code-walkthrough/python-walkthrough.md)通读代码
2. 重点理解 `workflow.py`（LangGraph编排）和 `monitor_agent.py`
3. 尝试本地运行 Python 版

### 阶段三：深入理解（2-3天）

1. 阅读八股文题库的技术栈深度和系统设计部分
2. 对比三种语言实现的差异
3. 思考如果是你会怎么设计（面试时被问到"如果重新做"）

### 阶段四：面试准备（2-3天）

1. 背诵[STAR法则模板](docs/interview/star-method.md)（特别是模板一和模板二）
2. 根据目标岗位选择[简历模板](docs/interview/resume-template.md)
3. 练习[面试常见问题](docs/interview/common-questions.md)的回答（建议对镜子练习）

### 阶段五：进阶加分（可选）

1. Fork本仓库，添加新功能（如Dashboard前端、Trend Agent）
2. 写一篇技术博客讲解你的理解
3. 给LangGraph/CrewAI提交一个PR（最高含金量的简历加分）

---

## 技术选型对比

### Agent框架选型

| 框架 | Stars | 核心概念 | 适用场景 | 学习曲线 |
|------|-------|---------|---------|---------|
| [LangGraph](https://github.com/langchain-ai/langgraph) | 28.5k | 图状态机 | 企业级复杂Pipeline | 中 |
| [CrewAI](https://github.com/CrewAIInc/crewai) | 48k | 角色化团队 | 3-8 Agent协作 | 低 |
| [AutoGen](https://github.com/microsoft/autogen) | 35k | 对话消息流 | 快速原型 | 低 |
| [AgentEnsemble](https://github.com/AgentEnsemble/agentensemble) | - | Java编排 | Java企业项目 | 中 |
| [KafClaw](https://github.com/KafClaw/KafClaw) | - | Go+Kafka | 分布式Agent | 高 |

### 参考的竞品情报开源项目

| 项目 | 亮点 |
|------|------|
| [CompetIQ](https://github.com/bhaveshsonje/competiq) | 5个并行Agent + Reflexion自评 + SSE |
| [serpapi/competitive-intelligence-agent](https://github.com/serpapi/competitive-intelligence-agent) | Plan-Execute-Synthesize + CRM集成 |
| [competitor_research_agent](https://github.com/ch1ns0n/competitor_research_agent) | A2A协议 + FAISS向量记忆 |

---

## FAQ

### Q: 这个项目需要付费的API吗？

A: 需要 OpenAI API Key（或兼容的LLM API）。GPT-4o 每天监控10个竞品约$1.3/天。没有Key也可以运行——搜索工具和Agent会使用Demo模式返回模拟数据。

### Q: 我是完全的小白，能看懂吗？

A: 可以。建议按照[学习路线图](#学习路线图)的顺序来，先理解概念再看代码。代码讲解文档会逐行解释。

### Q: 面试官会问"这是不是AI帮你写的"吗？

A: 很可能会。建议你：1）确保理解每一行代码；2）能解释每个设计决策；3）准备好被追问细节。详见[面试常见问题 Q25](docs/interview/common-questions.md)。

### Q: Python/Java/Go 应该选哪个版本准备面试？

A: 选你面试岗位对应的语言。如果岗位是AI/大模型相关，优先选Python版。

### Q: 可以在这个基础上添加新功能吗？

A: 当然可以，推荐添加：前端Dashboard、Trend Agent（趋势预测）、向量数据库集成、MCP协议支持。

---

## 参考资料

### 框架文档
- [LangGraph 官方文档](https://langchain-ai.github.io/langgraph/)
- [LangChain4j 文档](https://docs.langchain4j.dev/)
- [FastAPI 文档](https://fastapi.tiangolo.com/)
- [Gin 文档](https://gin-gonic.com/docs/)

### 学习资源
- [Building Event-Driven Multi-Agent Workflows with LangGraph](https://medium.com/@_Ankit_Malviya/building-event-driven-multi-agent-workflows-with-triggers-in-langgraph-48386c0aac5d)
- [Production-Ready Multi-Agent Systems with LangGraph](https://dev.to/sidkul2000/production-ready-multi-agent-systems-with-langgraph-a-complete-tutorial-20j1)
- [AI Agent面试全攻略](https://devpress.csdn.net/v1/article/detail/157729223)

### 面试准备
- [八股文50+题](docs/interview/eight-part-essay.md)
- [STAR法则模板](docs/interview/star-method.md)
- [简历写法](docs/interview/resume-template.md)
- [面试常见问题25题](docs/interview/common-questions.md)

---

## License

MIT License

---

> **Star** this repo if it helps you land the job!
