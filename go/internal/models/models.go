package models

import "time"

type ChangeType string

const (
	Pricing   ChangeType = "pricing"
	Product   ChangeType = "product"
	Hiring    ChangeType = "hiring"
	News      ChangeType = "news"
	Patent    ChangeType = "patent"
	Blog      ChangeType = "blog"
	OpenSource ChangeType = "open_source"
)

type Severity string

const (
	Low      Severity = "low"
	Medium   Severity = "medium"
	High     Severity = "high"
	Critical Severity = "critical"
)

type CompetitorChange struct {
	Competitor string     `json:"competitor"`
	ChangeType ChangeType `json:"change_type"`
	Title      string     `json:"title"`
	Summary    string     `json:"summary"`
	URL        string     `json:"url"`
	Severity   Severity   `json:"severity"`
	DetectedAt time.Time  `json:"detected_at"`
}

type ResearchInsight struct {
	Topic       string   `json:"topic"`
	Summary     string   `json:"summary"`
	KeyFindings []string `json:"key_findings"`
	Sources     []string `json:"sources"`
	Confidence  float64  `json:"confidence"`
}

type DimensionScore struct {
	Dimension       string  `json:"dimension"`
	OurScore        float64 `json:"our_score"`
	CompetitorScore float64 `json:"competitor_score"`
	Notes           string  `json:"notes"`
}

type ComparisonMatrix struct {
	Competitor        string           `json:"competitor"`
	Dimensions        []DimensionScore `json:"dimensions"`
	OverallAssessment string           `json:"overall_assessment"`
	GeneratedAt       time.Time        `json:"generated_at"`
}

type Battlecard struct {
	Competitor           string            `json:"competitor"`
	OurStrengths         []string          `json:"our_strengths"`
	OurWeaknesses        []string          `json:"our_weaknesses"`
	CompetitorStrengths  []string          `json:"competitor_strengths"`
	CompetitorWeaknesses []string          `json:"competitor_weaknesses"`
	KeyDifferentiators   []string          `json:"key_differentiators"`
	ObjectionHandling    map[string]string `json:"objection_handling"`
	ElevatorPitch        string            `json:"elevator_pitch"`
	GeneratedAt          time.Time         `json:"generated_at"`
}

type PipelineState struct {
	Competitor       string             `json:"competitor"`
	MonitorURLs      []string           `json:"monitor_urls,omitempty"`
	ChangesDetected  []CompetitorChange `json:"changes_detected"`
	ResearchResults  []ResearchInsight  `json:"research_results"`
	ComparisonMatrix *ComparisonMatrix  `json:"comparison_matrix,omitempty"`
	Battlecard       *Battlecard        `json:"battlecard,omitempty"`
	AlertsSent       []string           `json:"alerts_sent"`
	QualityScore     float64            `json:"quality_score"`
	ReflexionCount   int                `json:"reflexion_count"`
}
