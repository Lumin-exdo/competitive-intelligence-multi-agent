# Java 版代码讲解

> 本文档讲解 Java 版（Spring Boot + LangChain4j）的核心代码结构和设计思路。

---

## 项目结构

```
java/
├── pom.xml                          # Maven 依赖管理
├── src/main/java/com/ci/
│   ├── CIApplication.java           # Spring Boot 入口
│   ├── model/                       # 数据模型（对应Python的schemas.py）
│   │   ├── ChangeType.java          # 枚举
│   │   ├── Severity.java            # 枚举
│   │   ├── CompetitorChange.java    # 竞品变化
│   │   ├── ResearchInsight.java     # 研究洞察
│   │   ├── ComparisonMatrix.java    # 对比矩阵
│   │   ├── DimensionScore.java      # 维度评分
│   │   ├── Battlecard.java          # 战术卡
│   │   └── PipelineState.java       # Pipeline状态
│   ├── agents/                      # 5个Agent
│   │   ├── BaseAgent.java           # 抽象基类
│   │   ├── MonitorAgent.java
│   │   ├── ResearchAgent.java
│   │   ├── CompareAgent.java
│   │   ├── BattlecardAgent.java
│   │   └── AlertAgent.java
│   ├── workflow/
│   │   ├── CIPipeline.java          # Pipeline编排（核心）
│   │   └── QualityChecker.java      # 质量评估
│   ├── tools/
│   │   ├── WebScraper.java          # 网页爬取（Jsoup）
│   │   └── NotificationService.java # 通知推送
│   └── api/
│       └── CIController.java        # REST API
└── src/main/resources/
    └── application.yml              # Spring配置
```

---

## 核心设计模式

### 1. BaseAgent 抽象基类

```java
public abstract class BaseAgent {
    protected ChatLanguageModel buildLLM() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();
    }

    protected <T> T parseJson(String raw, TypeReference<T> ref) {
        // 统一的JSON解析 + markdown fence剥离
    }
}
```

所有Agent继承BaseAgent，获得LLM构建和JSON解析能力。新增Agent只需实现 `execute(PipelineState state)` 方法。

### 2. PipelineState — 可变状态对象

Java版使用可变的PipelineState对象在Agent间传递（vs Python版的TypedDict）：

```java
@Data @Builder
public class PipelineState {
    private String competitor;
    private List<CompetitorChange> changesDetected;
    private List<ResearchInsight> researchResults;
    private ComparisonMatrix comparisonMatrix;
    private Battlecard battlecard;
    private double qualityScore;
    private int reflexionCount;
}
```

Lombok的`@Data`自动生成getter/setter，`@Builder`提供链式构建。

### 3. CIPipeline — 编排核心

```java
public PipelineState run(String competitor) {
    PipelineState state = PipelineState.builder()
            .competitor(competitor).build();

    state = monitorAgent.execute(state);    // Step 1
    state = alertAgent.execute(state);      // Step 2 (并行)

    do {
        state = researchAgent.execute(state);    // Step 3
        state = compareAgent.execute(state);     // Step 4
        state = battlecardAgent.execute(state);  // Step 5
        score = qualityChecker.evaluate(state);  // Step 6
        state.setReflexionCount(state.getReflexionCount() + 1);
    } while (score < threshold && count < maxRetries);

    return state;
}
```

Java版没有LangGraph，所以用简单的do-while循环实现Reflexion。逻辑相同，实现更直观。

### 4. WebScraper — Jsoup

```java
Document doc = Jsoup.connect(url)
        .userAgent("Mozilla/5.0")
        .timeout(30_000)
        .get();
doc.select("script, style, noscript").remove();
return doc.body().text();
```

Jsoup是Java最流行的HTML解析库，等价于Python的BeautifulSoup。

---

## 与Python版的差异

| 维度 | Python | Java |
|------|--------|------|
| Agent框架 | LangGraph StateGraph | 自研 do-while 循环 |
| 并发 | asyncio | Virtual Thread (Java 21) |
| 模型定义 | Pydantic BaseModel | Lombok @Data |
| JSON解析 | json.loads + 容错 | Jackson + TypeReference |
| 依赖注入 | 手动实例化 | Spring @Component |
| 配置 | dotenv + dataclass | application.yml |
