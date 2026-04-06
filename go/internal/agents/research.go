package agents

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"github.com/competitive-intelligence/ci-agent/internal/models"
)

const researchSystemPrompt = `You are a Competitive Intelligence Research Agent.
Perform deep analysis covering: financials, patents, tech blogs, open-source, strategic moves.
Return JSON array: [{"topic":"...","summary":"...","key_findings":["..."],"sources":["..."],"confidence":0.8}].`

type ResearchAgent struct {
	llm *LLMClient
}

func NewResearchAgent(llm *LLMClient) *ResearchAgent {
	return &ResearchAgent{llm: llm}
}

func (a *ResearchAgent) Execute(ctx context.Context, state *models.PipelineState) error {
	changesSummary := "No specific changes – perform general research."
	if len(state.ChangesDetected) > 0 {
		lines := ""
		for _, c := range state.ChangesDetected {
			lines += fmt.Sprintf("- [%s] %s: %s\n", c.ChangeType, c.Title, c.Summary)
		}
		changesSummary = lines
	}

	userMsg := fmt.Sprintf("Competitor: %s\n\nDetected Changes:\n%s\nProvide research insights as JSON.",
		state.Competitor, changesSummary)

	response, err := a.llm.Chat(ctx, researchSystemPrompt, userMsg)
	if err != nil {
		return fmt.Errorf("research LLM error: %w", err)
	}

	var insights []models.ResearchInsight
	if err := ParseJSONResponse(response, &insights); err != nil {
		log.Printf("[Research] JSON parse error, storing raw: %v", err)
		state.ResearchResults = []models.ResearchInsight{{
			Topic:      "Raw Analysis",
			Summary:    response[:min(len(response), 2000)],
			Confidence: 0.5,
		}}
		return nil
	}

	state.ResearchResults = insights
	return nil
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
