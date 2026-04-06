package agents

import (
	"context"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/competitive-intelligence/ci-agent/internal/models"
	"github.com/competitive-intelligence/ci-agent/internal/tools"
)

const monitorSystemPrompt = `You are a Competitive Intelligence Monitor Agent.
Analyze web content and detect changes in pricing, products, hiring, and news.
Return a JSON array: [{"change_type":"pricing|product|hiring|news","title":"...","summary":"...","severity":"low|medium|high|critical","url":"..."}].
If no changes, return [].`

type MonitorAgent struct {
	llm *LLMClient
}

func NewMonitorAgent(llm *LLMClient) *MonitorAgent {
	return &MonitorAgent{llm: llm}
}

func (a *MonitorAgent) Execute(ctx context.Context, state *models.PipelineState) error {
	urls := state.MonitorURLs
	if len(urls) == 0 {
		urls = defaultURLs(state.Competitor)
	}

	for _, url := range urls {
		text, _, err := tools.FetchAndExtractText(url)
		if err != nil {
			log.Printf("[Monitor] Failed to fetch %s: %v", url, err)
			continue
		}

		if len(text) > 6000 {
			text = text[:6000]
		}

		userMsg := fmt.Sprintf("Competitor: %s\nURL: %s\n\nContent:\n%s\n\nReturn changes as JSON.",
			state.Competitor, url, text)

		response, err := a.llm.Chat(ctx, monitorSystemPrompt, userMsg)
		if err != nil {
			log.Printf("[Monitor] LLM error for %s: %v", url, err)
			continue
		}

		var items []struct {
			ChangeType string `json:"change_type"`
			Title      string `json:"title"`
			Summary    string `json:"summary"`
			Severity   string `json:"severity"`
			URL        string `json:"url"`
		}
		if err := ParseJSONResponse(response, &items); err != nil {
			log.Printf("[Monitor] JSON parse error: %v", err)
			continue
		}

		for _, item := range items {
			state.ChangesDetected = append(state.ChangesDetected, models.CompetitorChange{
				Competitor: state.Competitor,
				ChangeType: models.ChangeType(item.ChangeType),
				Title:      item.Title,
				Summary:    item.Summary,
				URL:        item.URL,
				Severity:   models.Severity(item.Severity),
				DetectedAt: time.Now().UTC(),
			})
		}
	}

	return nil
}

func defaultURLs(competitor string) []string {
	slug := strings.ToLower(strings.ReplaceAll(competitor, " ", ""))
	return []string{
		"https://" + slug + ".com",
		"https://" + slug + ".com/pricing",
		"https://" + slug + ".com/careers",
	}
}
