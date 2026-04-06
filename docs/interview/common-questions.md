# 面试常见问题及回答（25题）

> 覆盖面试中针对多Agent竞品情报系统项目的高频问题。每道题包含推荐回答和面试官的考察意图。

---

## 一、架构设计类（8题）

### Q1: 请介绍一下你这个项目的整体架构？

**考察意图：** 系统设计能力、全局观

**推荐回答：**
> 这是一个5-Agent的竞品情报系统，采用"事件驱动+Pipeline"混合编排模式。
>
> 整体是四层架构：
> - **API层**：FastAPI提供REST和SSE接口
> - **编排层**：LangGraph StateGraph做工作流编排
> - **Agent层**：5个专业化Agent（Monitor/Research/Compare/Battlecard/Alert）
> - **工具层**：爬虫、搜索、通知推送
>
> 数据流是：Monitor检测变化 → 同时触发Alert推送和Research深度分析 → Compare生成对比矩阵 → Battlecard生成战术卡 → Quality Check打分，低于7分回到Research重做。
>
> 基础设施用Kafka做事件传递、Elasticsearch存历史数据、Redis做缓存。

### Q2: 为什么设计5个Agent？能不能合并成更少的？

**考察意图：** 模块划分能力、SRP理解

**推荐回答：**
> 遵循单一职责原则，每个Agent有不同的：
> 1. **技能需求**：Monitor需要爬虫能力，Research需要分析能力，Battlecard需要销售话术能力
> 2. **运行频率**：Monitor每小时定时跑，Alert实时触发
> 3. **失败处理**：Monitor爬取失败不影响已有数据的分析流程
>
> 如果合并，比如把Monitor和Research合并，会导致：
> - Prompt过长，token浪费
> - 一个环节失败影响所有功能
> - 无法独立升级某个能力
>
> 当然，如果竞品数量很少（<5个），也可以简化为3个Agent（Monitor+Research、Compare+Battlecard、Alert）。

### Q3: 为什么选择LangGraph而不是CrewAI或AutoGen？

**考察意图：** 技术选型决策能力

**推荐回答：**
> 三个框架我都做了调研和PoC：
>
> **LangGraph胜出的原因：**
> 1. 我的Reflexion需要条件分支（quality<7→回到Research），LangGraph的图模型原生支持
> 2. 需要Pipeline+事件驱动混合模式，StateGraph可以同时有顺序边和并行扇出
> 3. 企业级需要checkpoint持久化（中途失败可恢复），LangGraph内建PostgreSQL/Redis checkpoint
>
> **不选CrewAI的原因：** 它的角色化模型很直观，但控制流是隐式的——我需要精确控制"先Monitor再Research"这种顺序
>
> **不选AutoGen的原因：** 它的对话流模型更适合探索性任务，不适合确定性的Pipeline工作流

### Q4: 事件驱动和Pipeline是怎么结合的？

**考察意图：** 架构模式理解

**推荐回答：**
> Pipeline是确定性顺序流：Monitor→Research→Compare→Battlecard，每个步骤必须等上一个完成。
>
> 事件驱动是非确定性触发：Monitor检测到HIGH/CRITICAL级别变化时，Alert Agent立即推送通知。
>
> 在LangGraph中的实现是：Monitor节点有两条出边——一条到Research（Pipeline），一条到Alert（事件触发）。这两条路径并行执行，互不阻塞。
>
> 这样设计的好处是：Alert可以第一时间通知团队，而不需要等整个分析Pipeline跑完。

### Q5: 如果让你支持100个竞品，架构需要怎么变？

**考察意图：** 可扩展性思考

**推荐回答：**
> 三个层面的调整：
>
> 1. **Monitor层**：从单实例变为分布式——用Kafka的"competitor-tasks" topic发布监控任务，多个Monitor Worker竞争消费。同时引入分级监控：TOP 10竞品每小时、其他每天。
>
> 2. **Pipeline层**：改为全异步——Monitor产出的变化事件投入Kafka，Research/Compare/Battlecard各自异步消费，不再是同步Pipeline。
>
> 3. **数据层**：ES分index存储，Dashboard支持全局搜索和对比。
>
> 核心思想是从"单次同步Pipeline"变为"事件驱动的异步微服务"。

### Q6: Kafka在系统中起什么作用？能不能用Redis替代？

**考察意图：** 中间件选型深度

**推荐回答：**
> Kafka在系统中有四个作用：
> 1. **解耦**：Agent之间通过topic通信，不直接调用
> 2. **持久化**：所有事件可回溯，支持审计
> 3. **削峰**：Monitor批量产出，Research按能力消费
> 4. **广播**：一条变化事件可以被多个消费者处理
>
> Redis Streams可以替代吗？**小规模可以，大规模不推荐**：
> - Redis是内存存储，大量历史事件存储成本高
> - Redis的Consumer Group功能相比Kafka较弱
> - Kafka的分区机制天然支持水平扩展
>
> 不过在PoC阶段，我确实用Redis Streams做过快速验证，后面才切到Kafka。

### Q7: 系统怎么保证高可用？

**考察意图：** 生产级思维

**推荐回答：**
> 三个层面：
>
> **应用层**：
> - FastAPI多实例部署，Nginx负载均衡
> - Agent执行有重试机制（tenacity，指数退避）
> - Reflexion有最大重试次数限制，防止无限循环
>
> **中间件层**：
> - Kafka 3节点集群，topic replica factor=3
> - ES 3节点集群，primary + replica shard
> - Redis Sentinel做主从切换
>
> **降级层**：
> - LLM不可用→返回缓存的上次分析
> - 爬虫失败→用搜索引擎替代
> - 通知渠道失败→自动切换备用渠道

### Q8: 安全性怎么考虑的？

**考察意图：** 安全意识

**推荐回答：**
> 五个方面：
> 1. **密钥管理**：所有API Key通过环境变量注入，生产用K8s Secret或Vault
> 2. **输入校验**：Pydantic做请求体校验，防止非法输入
> 3. **Prompt注入**：用户输入不直接拼到System Prompt，有独立的User Message
> 4. **数据合规**：只采集公开信息，不绕过登录/付费墙
> 5. **API限流**：FastAPI中间件限制请求频率

---

## 二、技术深度类（8题）

### Q9: Reflexion机制具体怎么实现的？

**考察意图：** 核心机制理解深度

**推荐回答：**
> 三步：
>
> 1. **生成**：Battlecard Agent根据对比矩阵和研究结果生成战术卡
> 2. **评估**：独立的Quality Check节点用另一个LLM调用，从三个维度打分：
>    - 完整性：优劣势、异议处理是否完整
>    - 准确性：是否与对比数据一致
>    - 可操作性：销售人员能否直接使用
> 3. **决策**：分数≥7 → 输出最终结果；分数<7 → 回到Research重新收集数据分析
>
> 代码层面是LangGraph的条件边：
> ```python
> graph.add_conditional_edges("quality_check",
>     lambda s: "research" if s["quality_score"] < 7 else END)
> ```
>
> 关键设计：最多重试3次防止无限循环；每次重试Research能看到之前的结果，会尝试不同角度分析。

### Q10: LLM输出的JSON格式怎么保证正确？

**考察意图：** 工程细节

**推荐回答：**
> 四层保障：
>
> 1. **Prompt约束**："Return ONLY a JSON array..."，并给出schema示例
> 2. **格式清洗**：LLM经常加```json标记，解析前先strip掉
>    ```python
>    if text.startswith("```"):
>        text = text.split("\n", 1)[1].rsplit("```", 1)[0]
>    ```
> 3. **Pydantic校验**：解析后的dict用Pydantic model验证字段类型和约束
> 4. **Fallback**：解析失败返回默认对象（如空的Battlecard + 原始文本作为elevator_pitch）
>
> 更高级的方案是用OpenAI的结构化输出（response_format），但我选择了通用方案以支持切换不同LLM provider。

### Q11: 变化检测的具体实现逻辑？

**考察意图：** 算法/工程实现

**推荐回答：**
> 三层检测策略：
>
> **第一层：Hash快速过滤**
> ```python
> new_hash = sha256(page_content)
> if new_hash == redis.get(f"hash:{url}"): return "no change"
> ```
> 大多数页面每次爬取内容一样，Hash对比可以快速排除。
>
> **第二层：结构化提取**
> 用CSS选择器提取定价信息和招聘列表等关键内容，做精确比对。
>
> **第三层：LLM语义分析**
> Hash变化但不确定是否有意义（可能只是广告位变了），交给LLM判断是否有业务价值的变化。
>
> 这样设计的好处是：90%的无变化情况在第一层就过滤了，节省LLM调用成本。

### Q12: 如何处理竞品网站的反爬？

**考察意图：** 实际问题解决能力

**推荐回答：**
> 分级应对：
>
> 1. **基础**：随机User-Agent、请求间隔2-5秒、跟随重定向
> 2. **中级**：代理IP池轮换、Cookie管理
> 3. **高级**：Playwright无头浏览器处理JS渲染
> 4. **替代方案**：优先使用RSS Feed、官方API；爬取失败降级到搜索引擎获取公开信息
>
> 关键原则：我们只采集公开信息，不绕过任何认证或付费墙。
>
> 实际上，大部分竞品的Pricing页和Blog是静态HTML，基础策略就够了。

### Q13: Agent的Prompt是怎么设计的？

**考察意图：** Prompt Engineering能力

**推荐回答：**
> 每个Agent的System Prompt遵循四要素：
>
> 1. **角色定义**：`"You are a Competitive Intelligence Monitor Agent"`
> 2. **任务说明**：具体要分析什么、输出什么
> 3. **输出格式**：精确到JSON字段名、类型、取值范围
> 4. **约束条件**：如"If no changes, return []"
>
> 以Monitor Agent为例，我约束了：
> - 变化类型只能是4种（pricing/product/hiring/news）
> - 严重程度只能是4级（low/medium/high/critical）
> - 必须返回JSON array
>
> 这样做的好处是下游Agent可以稳定地解析上游输出。
>
> Prompt的迭代方法：先写初版→跑10个测试case→看失败case调整prompt→重复。

### Q14: 如何估算和优化LLM API成本？

**考察意图：** 成本意识

**推荐回答：**
> 当前成本：10个竞品/天，约260K tokens，GPT-4o约$1.3/天（$40/月）。
>
> 优化手段：
> 1. **分级模型**：Monitor和Quality Check用GPT-4o-mini（便宜10倍），Research和Battlecard用GPT-4o
> 2. **内容截断**：网页内容只取前6000字符
> 3. **缓存**：相同查询结果缓存在Redis，TTL 24小时
> 4. **Hash过滤**：90%无变化的页面不调用LLM
> 5. **批量处理**：多个竞品的Research可以合并成一次LLM调用

### Q15: Python/Java/Go三个版本有什么差异？

**考察意图：** 多语言能力、技术选型

**推荐回答：**
> 核心架构一致，实现方式各有特色：
>
> | 维度 | Python | Java | Go |
> |------|--------|------|----|
> | Agent框架 | LangGraph | LangChain4j+Spring | 自研 |
> | 并发模型 | asyncio | Virtual Thread | goroutine |
> | 序列化 | Pydantic | Lombok+Jackson | struct tag |
> | 部署形态 | Docker | JAR | 单二进制 |
>
> Python版最完整（有SSE、Reflexion、Docker Compose），是主版本。
> Java版适合企业级Java技术栈的团队。
> Go版最轻量，适合部署资源受限的场景。

### Q16: 如何保证系统的可观测性？

**考察意图：** 生产运维能力

**推荐回答：**
> 三个层面：
>
> **日志**：structlog结构化日志，每条包含agent_name、competitor、run_id、duration_ms
>
> **指标**：Prometheus + Grafana
> - Pipeline端到端延迟（P50/P95/P99）
> - 每个Agent的LLM调用延迟
> - Reflexion重试率
> - 爬虫成功率
> - 告警发送成功率
>
> **追踪**：LangSmith集成，可以看到每次Pipeline运行中每个Agent的prompt、response、token数、耗时

---

## 三、编码能力类（5题）

### Q17: LangGraph的StateGraph具体怎么用的？

**推荐回答：**
> StateGraph的核心是三个概念：
>
> 1. **State定义**：TypedDict + Annotated Reducer
>    ```python
>    class PipelineState(TypedDict):
>        changes_detected: Annotated[list, _merge_lists]  # 追加模式
>        comparison_matrix: dict  # 覆盖模式
>    ```
>
> 2. **Node**：每个Agent是一个async函数，接收state dict，返回要更新的字段
>    ```python
>    async def monitor_agent(state: dict) -> dict:
>        return {"changes_detected": [change1, change2]}
>    ```
>
> 3. **Edge**：定义节点间的流转，包括无条件边和条件边
>    ```python
>    graph.add_edge("monitor", "research")  # 无条件
>    graph.add_conditional_edges("quality_check", condition_fn)  # 条件
>    ```

### Q18: Java版的Pipeline编排是怎么实现的？

**推荐回答：**
> Java版没有LangGraph等价物，所以我用了简单的顺序调用+循环实现Reflexion：
>
> ```java
> do {
>     state = researchAgent.execute(state);
>     state = compareAgent.execute(state);
>     state = battlecardAgent.execute(state);
>     score = qualityChecker.evaluate(state);
>     state.setReflexionCount(state.getReflexionCount() + 1);
> } while (score < threshold && count < maxRetries);
> ```
>
> 每个Agent的execute方法接收PipelineState、修改后返回。
> 设计了BaseAgent抽象类统一LLM交互和JSON解析逻辑。

### Q19: Go版的Agent间通信是怎么设计的？

**推荐回答：**
> Go版目前用的是进程内直接调用（适合单机部署）。如果要分布式，每个Agent消费自己的Kafka topic：
>
> ```go
> // Monitor 产出 → ci.changes topic
> // Research 消费 ci.changes → 产出 → ci.analysis topic
> // Compare 消费 ci.analysis → 产出 → ci.comparison topic
> ```
>
> Go的goroutine天然适合并发：Monitor+Alert可以用两个goroutine并行执行。

### Q20: 如何测试一个Agent的输出质量？

**推荐回答：**
> 三层测试：
>
> 1. **单元测试**：验证数据模型和解析逻辑（不调用LLM）
> 2. **集成测试**：Mock LLM返回，验证Agent的完整处理流程
> 3. **质量评估**：准备10个标准测试case，用LangSmith Evaluator自动打分
>
> 比如测试Monitor Agent：
> - 准备一个包含定价变化的HTML
> - Mock fetch_page返回这个HTML
> - 验证Agent能正确识别出pricing类型的变化

---

## 四、个人成长类（4题）

### Q21: 通过这个项目你学到了什么？

**推荐回答：**
> 三个核心收获：
>
> 1. **Agent系统设计**：深入理解了多Agent协作的模式和挑战，特别是如何处理LLM输出的不确定性
> 2. **Prompt Engineering**：从"随便写"到"结构化设计"，学会了用约束和格式要求来稳定LLM输出
> 3. **系统思维**：一个好的AI系统不只是调API，还需要完善的错误处理、监控、降级策略

### Q22: 如果重新做一遍，你会改什么？

**推荐回答：**
> 三个改进：
> 1. **引入MCP协议**：工具层统一用Model Context Protocol，让Agent可以动态发现和使用工具
> 2. **添加前端Dashboard**：目前只有API，缺少可视化界面展示竞品动态趋势
> 3. **使用向量数据库**：把ES的全文检索升级为语义搜索，提升历史数据检索的准确性

### Q23: 你怎么看AI Agent的发展趋势？

**推荐回答：**
> 2025-2026年的几个趋势：
> 1. **Agent协议标准化**：A2A（Agent-to-Agent）和MCP正在成为行业标准
> 2. **从Demo到Production**：框架从"能跑"进化到"能用"——持久化、监控、治理
> 3. **多模态Agent**：不只处理文本，还能理解图片/视频（如分析竞品UI截图）
> 4. **成本下降**：模型推理成本持续下降，使Agent系统更经济可行

### Q24: 为什么选择做这个项目而不是其他Agent项目？

**推荐回答：**
> 三个原因：
> 1. **业务价值明确**：竞品情报是每个企业都需要的，面试时容易让面试官理解
> 2. **技术栈全面**：涵盖爬虫、搜索、LLM、消息队列、搜索引擎、缓存、通知——体现全栈能力
> 3. **Agent数量合适**：5个Agent既能展示多Agent协作，又不会过于复杂——面试30分钟能讲清楚

---

## 五、压力面试类

### Q25: 你这个项目是自己做的还是AI帮你做的？

**推荐回答：**
> 架构设计和技术选型是我自己做的——我调研了LangGraph、CrewAI、AutoGen，做了PoC对比后选的LangGraph。
>
> 编码过程中我确实用了AI辅助工具（Cursor），但核心逻辑（工作流编排、Reflexion机制、变化检测策略）都是我设计的。
>
> 我的判断是：AI辅助编码是提升效率的工具，关键是你要能理解每一行代码、能解释每个设计决策、能调试每个Bug。就像用IDE的自动补全一样，重要的是你的系统设计能力和工程判断。
>
> （然后准备好被追问代码细节——确保你真的理解所有代码）
