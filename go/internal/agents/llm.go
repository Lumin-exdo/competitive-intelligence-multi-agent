package agents

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	openai "github.com/sashabaranov/go-openai"
)

type LLMClient struct {
	client *openai.Client
	model  string
	temp   float32
}

func NewLLMClient(apiKey, model string, temp float32) *LLMClient {
	return &LLMClient{
		client: openai.NewClient(apiKey),
		model:  model,
		temp:   temp,
	}
}

func (l *LLMClient) Chat(ctx context.Context, systemPrompt, userMessage string) (string, error) {
	resp, err := l.client.CreateChatCompletion(ctx, openai.ChatCompletionRequest{
		Model:       l.model,
		Temperature: l.temp,
		Messages: []openai.ChatCompletionMessage{
			{Role: openai.ChatMessageRoleSystem, Content: systemPrompt},
			{Role: openai.ChatMessageRoleUser, Content: userMessage},
		},
	})
	if err != nil {
		return "", fmt.Errorf("LLM call failed: %w", err)
	}
	if len(resp.Choices) == 0 {
		return "", fmt.Errorf("no response from LLM")
	}
	return resp.Choices[0].Message.Content, nil
}

// ParseJSONResponse strips markdown code fences and parses JSON.
func ParseJSONResponse(raw string, target interface{}) error {
	text := strings.TrimSpace(raw)
	if strings.HasPrefix(text, "```") {
		lines := strings.SplitN(text, "\n", 2)
		if len(lines) == 2 {
			text = lines[1]
		}
		if idx := strings.LastIndex(text, "```"); idx > 0 {
			text = text[:idx]
		}
	}
	return json.Unmarshal([]byte(text), target)
}
