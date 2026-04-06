package agents

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"github.com/competitive-intelligence/ci-agent/internal/models"
)

const compareSystemPrompt = `You are a Comparison Agent. Score both our product and the competitor (0-10)
across: Product Features, Pricing, UX, Market Share, Sentiment, Technology, Ecosystem, Support.
Return JSON: {"dimensions":[{"dimension":"...","our_score":8,"competitor_score":6,"notes":"..."}],"overall_assessment":"..."}.`

type CompareAgent struct {
	llm *LLMClient
}

func NewCompareAgent(llm *LLMClient) *CompareAgent {
	return &CompareAgent{llm: llm}
}

func (a *CompareAgent) Execute(ctx context.Context, state *models.PipelineState) error {
	researchJSON, _ := json.MarshalIndent(state.ResearchResults, "", "  ")

	userMsg := fmt.Sprintf("Competitor: %s\n\nResearch:\n%s\n\nGenerate comparison matrix as JSON.",
		state.Competitor, string(researchJSON))

	response, err := a.llm.Chat(ctx, compareSystemPrompt, userMsg)
	if err != nil {
		return fmt.Errorf("compare LLM error: %w", err)
	}

	var matrix struct {
		Dimensions        []models.DimensionScore `json:"dimensions"`
		OverallAssessment string                  `json:"overall_assessment"`
	}
	if err := ParseJSONResponse(response, &matrix); err != nil {
		log.Printf("[Compare] JSON parse error: %v", err)
		return nil
	}

	state.ComparisonMatrix = &models.ComparisonMatrix{
		Competitor:        state.Competitor,
		Dimensions:        matrix.Dimensions,
		OverallAssessment: matrix.OverallAssessment,
	}
	return nil
}
