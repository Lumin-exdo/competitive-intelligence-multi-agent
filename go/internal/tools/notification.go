package tools

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"
)

type Notifier struct {
	SlackWebhook    string
	DingTalkWebhook string
}

func NewNotifier(slackURL, dingtalkURL string) *Notifier {
	return &Notifier{
		SlackWebhook:    slackURL,
		DingTalkWebhook: dingtalkURL,
	}
}

func (n *Notifier) Broadcast(title, message string) map[string]bool {
	result := make(map[string]bool)
	result["slack"] = n.sendSlack(title, message)
	result["dingtalk"] = n.sendDingTalk(title, message)
	return result
}

func (n *Notifier) sendSlack(title, message string) bool {
	if n.SlackWebhook == "" {
		return false
	}
	payload := map[string]string{
		"text": fmt.Sprintf("🚨 *%s*\n%s", title, message),
	}
	return postJSON(n.SlackWebhook, payload)
}

func (n *Notifier) sendDingTalk(title, message string) bool {
	if n.DingTalkWebhook == "" {
		return false
	}
	payload := map[string]interface{}{
		"msgtype": "text",
		"text": map[string]string{
			"content": fmt.Sprintf("🚨 %s\n%s", title, message),
		},
	}
	return postJSON(n.DingTalkWebhook, payload)
}

func postJSON(url string, payload interface{}) bool {
	data, err := json.Marshal(payload)
	if err != nil {
		log.Printf("JSON marshal error: %v", err)
		return false
	}

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Post(url, "application/json", bytes.NewReader(data))
	if err != nil {
		log.Printf("HTTP POST error: %v", err)
		return false
	}
	defer resp.Body.Close()
	return resp.StatusCode == 200
}
