package agents

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/competitive-intelligence/ci-agent/internal/models"
)

const battlecardSystemPrompt = `You are a Sales Battlecard Generator.
Return JSON: {"our_strengths":["..."],"our_weaknesses":["..."],"competitor_strengths":["..."],
"competitor_weaknesses":["..."],"key_differentiators":["..."],"objection_handling":{"objection":"response"},
"elevator_pitch":"..."}.`

type BattlecardAgent struct {
	llm *LLMClient
}

func NewBattlecardAgent(llm *LLMClient) *BattlecardAgent {
	return &BattlecardAgent{llm: llm}
}

func (a *BattlecardAgent) Execute(ctx context.Context, state *models.PipelineState) error {
	comparisonJSON, _ := json.MarshalIndent(state.ComparisonMatrix, "", "  ")
	researchJSON, _ := json.MarshalIndent(state.ResearchResults, "", "  ")

	userMsg := fmt.Sprintf("Competitor: %s\n\nComparison:\n%s\n\nResearch:\n%s\n\nGenerate battlecard as JSON.",
		state.Competitor, string(comparisonJSON), string(researchJSON))

	response, err := a.llm.Chat(ctx, battlecardSystemPrompt, userMsg)
	if err != nil {
		return fmt.Errorf("battlecard LLM error: %w", err)
	}

	var card models.Battlecard
	if err := ParseJSONResponse(response, &card); err != nil {
		log.Printf("[Battlecard] JSON parse error: %v", err)
		state.Battlecard = &models.Battlecard{
			Competitor:    state.Competitor,
			ElevatorPitch: response[:min(len(response), 500)],
		}
		return nil
	}

	card.Competitor = state.Competitor
	card.GeneratedAt = time.Now().UTC()
	state.Battlecard = &card
	return nil
}
