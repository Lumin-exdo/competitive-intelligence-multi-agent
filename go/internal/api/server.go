package api

import (
	"net/http"
	"time"

	"github.com/competitive-intelligence/ci-agent/internal/config"
	"github.com/competitive-intelligence/ci-agent/internal/pipeline"
	"github.com/gin-gonic/gin"
)

type Server struct {
	router   *gin.Engine
	pipeline *pipeline.Pipeline
	cfg      *config.Config
}

func New(cfg *config.Config) *Server {
	s := &Server{
		router:   gin.Default(),
		pipeline: pipeline.New(cfg),
		cfg:      cfg,
	}
	s.routes()
	return s
}

func (s *Server) routes() {
	s.router.GET("/health", s.health)
	s.router.POST("/analyze", s.analyze)
}

func (s *Server) Run() error {
	return s.router.Run(":" + s.cfg.ServerPort)
}

func (s *Server) health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status":    "ok",
		"timestamp": time.Now().UTC().Format(time.RFC3339),
	})
}

type analyzeRequest struct {
	Competitor string `json:"competitor" binding:"required"`
}

func (s *Server) analyze(c *gin.Context) {
	var req analyzeRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	state, err := s.pipeline.Run(c.Request.Context(), req.Competitor)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, state)
}
