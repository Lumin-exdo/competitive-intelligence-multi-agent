package main

import (
	"log"

	"github.com/competitive-intelligence/ci-agent/internal/api"
	"github.com/competitive-intelligence/ci-agent/internal/config"
)

func main() {
	cfg := config.Load()
	log.Printf("Starting CI Multi-Agent System on port %s", cfg.ServerPort)

	server := api.New(cfg)
	if err := server.Run(); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}
