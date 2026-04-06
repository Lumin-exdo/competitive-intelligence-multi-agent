# Go 版代码讲解

> 本文档讲解 Go 版（Gin + Kafka 事件驱动）的核心代码结构。

---

## 项目结构

```
go/
├── go.mod                              # Go 模块定义
├── cmd/server/
│   └── main.go                         # 入口文件
└── internal/                           # 内部包（不对外暴露）
    ├── config/config.go                # 配置管理
    ├── models/models.go                # 数据模型
    ├── agents/
    │   ├── llm.go                      # LLM客户端封装
    │   ├── monitor.go                  # Monitor Agent
    │   ├── research.go                 # Research Agent
    │   ├── compare.go                  # Compare Agent
    │   ├── battlecard.go               # Battlecard Agent
    │   └── alert.go                    # Alert Agent
    ├── pipeline/pipeline.go            # Pipeline编排
    ├── tools/
    │   ├── scraper.go                  # 网页爬取（goquery）
    │   └── notification.go             # 通知推送
    └── api/server.go                   # Gin HTTP服务
```

Go遵循 `internal/` 包约定——internal下的代码只能被同模块内的代码import。

---

## 核心设计

### 1. LLMClient — 通用LLM封装

```go
type LLMClient struct {
    client *openai.Client
    model  string
    temp   float32
}

func (l *LLMClient) Chat(ctx context.Context, systemPrompt, userMessage string) (string, error) {
    resp, err := l.client.CreateChatCompletion(ctx, openai.ChatCompletionRequest{
        Model: l.model,
        Messages: []openai.ChatCompletionMessage{
            {Role: openai.ChatMessageRoleSystem, Content: systemPrompt},
            {Role: openai.ChatMessageRoleUser, Content: userMessage},
        },
    })
    return resp.Choices[0].Message.Content, nil
}
```

使用 `go-openai` 库，所有Agent共享同一个LLMClient实例。

### 2. Agent 实现模式

每个Agent遵循相同模式：

```go
type ResearchAgent struct {
    llm *LLMClient
}

func (a *ResearchAgent) Execute(ctx context.Context, state *models.PipelineState) error {
    // 1. 从state读取输入
    // 2. 构造prompt
    // 3. 调用LLM
    // 4. 解析JSON响应
    // 5. 写回state
    return nil
}
```

使用指针接收者 `*PipelineState`，Agent直接修改state对象（Go惯用方式）。

### 3. Pipeline 编排

```go
func (p *Pipeline) Run(ctx context.Context, competitor string) (*models.PipelineState, error) {
    state := &models.PipelineState{Competitor: competitor}

    p.monitor.Execute(ctx, state)
    p.alert.Execute(state)          // 不需要LLM，不需要ctx

    for {
        p.research.Execute(ctx, state)
        p.compare.Execute(ctx, state)
        p.battlecard.Execute(ctx, state)

        score := p.evaluateQuality(ctx, state)
        state.QualityScore = score
        state.ReflexionCount++

        if score >= threshold || state.ReflexionCount >= maxRetries {
            break
        }
    }
    return state, nil
}
```

Go的for循环实现Reflexion，简洁明了。

### 4. goquery — Go的jQuery

```go
doc, _ := goquery.NewDocumentFromReader(resp.Body)
doc.Find("script, style, noscript").Remove()
text := doc.Find("body").Text()
hash := fmt.Sprintf("%x", sha256.Sum256([]byte(text)))
```

goquery提供类似jQuery的CSS选择器API，等价于Python的BeautifulSoup。

### 5. Gin HTTP服务

```go
func (s *Server) analyze(c *gin.Context) {
    var req analyzeRequest
    c.ShouldBindJSON(&req)

    state, err := s.pipeline.Run(c.Request.Context(), req.Competitor)
    c.JSON(http.StatusOK, state)
}
```

Gin的上下文传递：通过 `c.Request.Context()` 将HTTP请求的context传递给Pipeline，支持请求取消。

---

## Go 版的独特优势

| 优势 | 说明 |
|------|------|
| 单二进制部署 | `go build` 产出一个二进制文件，无需运行时环境 |
| 低内存占用 | 单次分析 <100MB 内存 |
| 快速启动 | <100ms 启动时间 |
| 原生并发 | goroutine 天然适合并行执行多个Agent |
| 类型安全 | 编译期发现错误 |

---

## 与Python/Java版的对比

| 维度 | Python | Java | Go |
|------|--------|------|----|
| 代码量 | 最多 | 中等 | 最少 |
| 框架依赖 | LangGraph | Spring Boot | Gin (轻量) |
| 部署复杂度 | Docker | JAR + JVM | 单文件 |
| 适用场景 | 功能最全 | 企业Java栈 | 高性能/微服务 |
