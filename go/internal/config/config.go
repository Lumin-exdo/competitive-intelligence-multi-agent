package config

import (
	"os"
	"strconv"

	"github.com/joho/godotenv"
)

type Config struct {
	LLM          LLMConfig
	Kafka        KafkaConfig
	Notification NotificationConfig
	Pipeline     PipelineConfig
	ServerPort   string
}

type LLMConfig struct {
	APIKey      string
	Model       string
	Temperature float32
}

type KafkaConfig struct {
	BootstrapServers string
	GroupID          string
	TopicChanges     string
	TopicAnalysis    string
	TopicComparison  string
	TopicBattlecard  string
	TopicAlerts      string
}

type NotificationConfig struct {
	SlackWebhook    string
	DingTalkWebhook string
}

type PipelineConfig struct {
	QualityThreshold float64
	MaxRetries       int
}

func Load() *Config {
	_ = godotenv.Load()

	threshold, _ := strconv.ParseFloat(getEnv("QUALITY_THRESHOLD", "7.0"), 64)
	maxRetries, _ := strconv.Atoi(getEnv("MAX_REFLEXION_RETRIES", "3"))
	temp, _ := strconv.ParseFloat(getEnv("LLM_TEMPERATURE", "0.3"), 32)

	return &Config{
		LLM: LLMConfig{
			APIKey:      getEnv("OPENAI_API_KEY", ""),
			Model:       getEnv("LLM_MODEL", "gpt-4o"),
			Temperature: float32(temp),
		},
		Kafka: KafkaConfig{
			BootstrapServers: getEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
			GroupID:          getEnv("KAFKA_GROUP_ID", "ci-agents-go"),
			TopicChanges:     "ci.changes",
			TopicAnalysis:    "ci.analysis",
			TopicComparison:  "ci.comparison",
			TopicBattlecard:  "ci.battlecard",
			TopicAlerts:      "ci.alerts",
		},
		Notification: NotificationConfig{
			SlackWebhook:    getEnv("SLACK_WEBHOOK_URL", ""),
			DingTalkWebhook: getEnv("DINGTALK_WEBHOOK_URL", ""),
		},
		Pipeline: PipelineConfig{
			QualityThreshold: threshold,
			MaxRetries:       maxRetries,
		},
		ServerPort: getEnv("SERVER_PORT", "8090"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
