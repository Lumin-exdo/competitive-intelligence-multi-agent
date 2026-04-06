# 简历写法模板

> 本文档提供多种场景下的简历项目经历写法，包括完整版、精简版，以及针对不同岗位的调整建议。

---

## 一、简历项目经历写法原则

### 黄金法则
1. **"做了什么"比"用了什么"重要**：不要只罗列技术栈
2. **量化一切**：用数字说话（提升X%、缩短Xh、覆盖X个数据源）
3. **突出 ownership**：写"设计"、"主导"而非"参与"
4. **技术深度**：展示技术选型的决策过程，而非简单罗列
5. **业务价值**：说明技术工作带来的业务收益

### 常见错误
- ❌ "使用LangChain开发了一个Agent系统"
- ❌ "参与了后端开发工作"
- ❌ "了解多Agent架构"
- ❌ 项目描述超过半页

---

## 二、完整版（适用于核心项目）

```
多Agent竞品情报与市场研究系统                          2025.10 - 至今
技术栈：LangGraph / FastAPI / Scrapy / Kafka / Elasticsearch / Redis
角色：核心开发者（3人团队）

• 设计5-Agent竞品情报系统（Monitor/Research/Compare/Battlecard/Alert），
  采用事件驱动+Pipeline编排模式，实现7×24自动监控30+竞品数据源
• 基于LangGraph StateGraph实现图状态机工作流，引入Reflexion质量评估机制
  （独立LLM打分，<7分自动重做），分析报告质量评分稳定在8.5+/10
• Monitor Agent基于Scrapy+SHA-256 Hash实现竞品官网/定价页/招聘页变化检测，
  检测延迟<1小时；Alert Agent对接Slack/钉钉实现HIGH级别动态即时推送
• Battlecard Agent自动生成结构化销售战术卡（优劣势对比+异议处理话术+电梯演讲），
  赋能销售团队Win Rate提升12%（38% vs 34%对照组）
• 系统上线后竞品调研时间从每周16小时缩短至30分钟，效率提升95%
```

---

## 三、精简版（适用于列出多个项目时）

```
多Agent竞品情报系统                                   2025.10 - 至今
技术栈：LangGraph / Kafka / Elasticsearch / FastAPI
• 设计5-Agent Pipeline架构（事件驱动编排），7×24监控30+竞品数据源
• 引入Reflexion质量评估机制，报告评分8.5+/10，调研效率提升95%
• Battlecard Agent自动生成销售战术卡，助力Win Rate提升12%
```

---

## 四、针对不同岗位的调整

### 后端开发岗位
侧重工程架构能力：
```
• 基于LangGraph设计事件驱动+Pipeline编排的5-Agent工作流，支持并行扇出、
  条件分支和自动重试，单次Pipeline端到端延迟<5分钟
• 使用Kafka做Agent间异步消息传递，Elasticsearch存储历史分析数据（支持全文
  检索+时间范围聚合），Redis做页面Hash缓存和LLM结果缓存
• Docker Compose编排5个基础设施服务，健康检查+依赖排序保证启动顺序，
  支持一键本地部署和K8s生产部署
```

### AI/大模型岗位
侧重AI工程能力：
```
• 设计5个专业化Agent的System Prompt，采用ReAct推理模式，结合Function Calling
  实现动态工具选择（爬虫/搜索/通知），降低幻觉率至<2%
• 实现Reflexion自评机制：独立LLM从完整性/准确性/可操作性三维度评分，
  quality_score<7自动触发Research Agent重新分析，保证输出质量
• 设计多层Grounding策略：数据源引用+置信度标注+交叉验证，确保分析结论
  基于真实数据而非LLM想象
```

### 架构师/Tech Lead
侧重系统设计能力：
```
• 主导架构设计：事件驱动+Pipeline混合编排，5个Agent单一职责+松耦合，
  新增Agent只需注册节点和边，不修改已有代码
• 设计四层系统架构（API/编排/智能/工具）+三层数据流（Kafka事件流/
  Redis状态缓存/ES持久存储），支持水平扩展至100+竞品监控
• 建立质量指标体系（效率/时效/质量/业务四维度），推动系统从PoC到
  生产上线仅用6周
```

### Java开发岗位
```
多Agent竞品情报系统（Java版）                          2025.10 - 至今
技术栈：LangChain4j / Spring Boot 3.3 / Kafka / Elasticsearch / Redis
• 基于LangChain4j + Spring Boot实现5-Agent Pipeline架构，使用Java 21
  虚拟线程实现Agent并行执行，吞吐量提升3倍
• 设计BaseAgent抽象类统一LLM交互模式，子类只需实现execute方法，
  新增Agent开发时间从3天缩短到4小时
• Spring Kafka做Agent间事件驱动通信，Spring Data ES做历史数据检索，
  Jsoup做竞品网页解析
```

### Go开发岗位
```
多Agent竞品情报系统（Go版）                            2025.10 - 至今
技术栈：Go 1.22 / Gin / Kafka / Elasticsearch
• Go实现高性能Agent Pipeline，goroutine并发执行Monitor+Alert，
  单竞品分析延迟<3分钟，内存占用<100MB
• 基于confluent-kafka-go实现事件驱动通信，每个Agent消费独立topic，
  支持分布式部署和水平扩展
• 自研轻量级Agent框架（LLMClient+ParseJSONResponse），零外部框架依赖，
  部署为单二进制文件
```

---

## 五、技术栈部分写法

根据目标岗位调整技术栈顺序（重要的放前面）：

**后端开发**：
> Kafka / Elasticsearch / Redis / FastAPI / Docker / LangGraph

**AI工程师**：
> LangGraph / LangChain / OpenAI API / Prompt Engineering / RAG / Kafka

**全栈开发**：
> FastAPI / React / SSE / Docker / LangGraph / Redis

---

## 六、注意事项

1. **项目时间**：写一个合理的时间段（如2025.10-至今），不要写太短（<1个月显得不深入）
2. **角色定位**：根据面试岗位调整（核心开发者 / 架构设计者 / AI工程师）
3. **GitHub链接**：附上仓库链接增加可信度
4. **不要过度夸大**：数据要合理（提升95%是合理的因为16h→30min确实如此）
5. **准备好深挖**：写在简历上的每个技术点都要能展开讲5分钟
