package agents

import (
	"fmt"
	"log"

	"github.com/competitive-intelligence/ci-agent/internal/models"
	"github.com/competitive-intelligence/ci-agent/internal/tools"
)

type AlertAgent struct {
	notifier *tools.Notifier
}

func NewAlertAgent(notifier *tools.Notifier) *AlertAgent {
	return &AlertAgent{notifier: notifier}
}

func (a *AlertAgent) Execute(state *models.PipelineState) {
	for _, change := range state.ChangesDetected {
		if change.Severity != models.High && change.Severity != models.Critical {
			continue
		}

		title := fmt.Sprintf("[%s] %s: %s", change.Severity, change.Competitor, change.Title)
		message := fmt.Sprintf("Type: %s\nSummary: %s\nURL: %s",
			change.ChangeType, change.Summary, change.URL)

		result := a.notifier.Broadcast(title, message)
		channels := ""
		for ch, ok := range result {
			if ok {
				channels += ch + ","
			}
		}
		state.AlertsSent = append(state.AlertsSent, title)
		log.Printf("[Alert] %s → channels: %s", title, channels)
	}
}
