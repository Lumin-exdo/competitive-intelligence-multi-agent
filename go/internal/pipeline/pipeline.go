package pipeline

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"github.com/competitive-intelligence/ci-agent/internal/agents"
	"github.com/competitive-intelligence/ci-agent/internal/config"
	"github.com/competitive-intelligence/ci-agent/internal/models"
	"github.com/competitive-intelligence/ci-agent/internal/tools"
)

// Pipeline orchestrates the 5-agent event-driven workflow.
type Pipeline struct {
	monitor    *agents.MonitorAgent
	research   *agents.ResearchAgent
	compare    *agents.CompareAgent
	battlecard *agents.BattlecardAgent
	alert      *agents.AlertAgent
	llm        *agents.LLMClient
	cfg        *config.Config
}

func New(cfg *config.Config) *Pipeline {
	llm := agents.NewLLMClient(cfg.LLM.APIKey, cfg.LLM.Model, cfg.LLM.Temperature)
	notifier := tools.NewNotifier(cfg.Notification.SlackWebhook, cfg.Notification.DingTalkWebhook)

	return &Pipeline{
		monitor:    agents.NewMonitorAgent(llm),
		research:   agents.NewResearchAgent(llm),
		compare:    agents.NewCompareAgent(llm),
		battlecard: agents.NewBattlecardAgent(llm),
		alert:      agents.NewAlertAgent(notifier),
		llm:        llm,
		cfg:        cfg,
	}
}

func (p *Pipeline) Run(ctx context.Context, competitor string) (*models.PipelineState, error) {
	state := &models.PipelineState{
		Competitor: competitor,
	}

	log.Printf("▶ Pipeline started for: %s", competitor)

	// Step 1: Monitor
	if err := p.monitor.Execute(ctx, state); err != nil {
		return state, fmt.Errorf("monitor failed: %w", err)
	}
	log.Printf("  ✓ Monitor: %d changes", len(state.ChangesDetected))

	// Step 2: Alert (independent)
	p.alert.Execute(state)
	log.Printf("  ✓ Alert: %d alerts", len(state.AlertsSent))

	// Step 3-5: Research → Compare → Battlecard with Reflexion
	for {
		if err := p.research.Execute(ctx, state); err != nil {
			return state, fmt.Errorf("research failed: %w", err)
		}
		log.Printf("  ✓ Research: %d insights", len(state.ResearchResults))

		if err := p.compare.Execute(ctx, state); err != nil {
			return state, fmt.Errorf("compare failed: %w", err)
		}
		log.Printf("  ✓ Compare: matrix generated")

		if err := p.battlecard.Execute(ctx, state); err != nil {
			return state, fmt.Errorf("battlecard failed: %w", err)
		}
		log.Printf("  ✓ Battlecard: generated")

		score := p.evaluateQuality(ctx, state)
		state.QualityScore = score
		state.ReflexionCount++
		log.Printf("  ✓ Quality: %.1f/10 (attempt %d/%d)",
			score, state.ReflexionCount, p.cfg.Pipeline.MaxRetries)

		if score >= p.cfg.Pipeline.QualityThreshold || state.ReflexionCount >= p.cfg.Pipeline.MaxRetries {
			break
		}
	}

	log.Printf("▶ Pipeline completed for: %s (quality: %.1f)", competitor, state.QualityScore)
	return state, nil
}

func (p *Pipeline) evaluateQuality(ctx context.Context, state *models.PipelineState) float64 {
	cardJSON, _ := json.MarshalIndent(state.Battlecard, "", "  ")

	prompt := fmt.Sprintf(
		"Rate this battlecard 1-10 on completeness, accuracy, actionability.\n"+
			"Return ONLY: {\"score\": <float>, \"feedback\": \"<string>\"}\n\n"+
			"Battlecard:\n%s", string(cardJSON))

	response, err := p.llm.Chat(ctx,
		"You are a strict quality evaluator.", prompt)
	if err != nil {
		return 5.0
	}

	var result struct {
		Score float64 `json:"score"`
	}
	if err := agents.ParseJSONResponse(response, &result); err != nil {
		return 5.0
	}
	return result.Score
}
