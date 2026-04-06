"""FastAPI server exposing the CI pipeline via REST + SSE endpoints."""

from __future__ import annotations

import asyncio
import json
import logging
from contextlib import asynccontextmanager
from datetime import datetime
from typing import AsyncGenerator

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from ..graph.workflow import pipeline, PipelineState

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Request / Response models
# ---------------------------------------------------------------------------

class AnalyzeRequest(BaseModel):
    competitor: str
    urls: list[str] | None = None


class AnalyzeResponse(BaseModel):
    competitor: str
    changes_detected: list = []
    research_results: list = []
    comparison_matrix: dict | None = None
    battlecard: dict | None = None
    alerts_sent: list = []
    quality_score: float = 0.0


class HealthResponse(BaseModel):
    status: str
    timestamp: str


# ---------------------------------------------------------------------------
# Application factory
# ---------------------------------------------------------------------------

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("CI Multi-Agent Pipeline started")
    yield
    logger.info("CI Multi-Agent Pipeline shutting down")


app = FastAPI(
    title="Multi-Agent Competitive Intelligence System",
    description=(
        "Enterprise-grade CI system with 5 specialized agents: "
        "Monitor, Research, Compare, Battlecard, and Alert."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(status="ok", timestamp=datetime.utcnow().isoformat())


@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(req: AnalyzeRequest):
    """Run the full pipeline synchronously and return the final state."""
    initial_state: PipelineState = {
        "competitor": req.competitor,
        "monitor_urls": req.urls or [],
        "previous_hashes": {},
        "changes_detected": [],
        "research_results": [],
        "comparison_matrix": {},
        "battlecard": {},
        "alerts_sent": [],
        "quality_score": 0.0,
        "reflexion_count": 0,
        "error": None,
    }

    try:
        final = await pipeline.ainvoke(initial_state)
    except Exception as exc:
        logger.exception("Pipeline failed")
        raise HTTPException(status_code=500, detail=str(exc))

    return AnalyzeResponse(
        competitor=final["competitor"],
        changes_detected=final.get("changes_detected", []),
        research_results=final.get("research_results", []),
        comparison_matrix=final.get("comparison_matrix"),
        battlecard=final.get("battlecard"),
        alerts_sent=final.get("alerts_sent", []),
        quality_score=final.get("quality_score", 0.0),
    )


@app.post("/analyze/stream")
async def analyze_stream(req: AnalyzeRequest):
    """Stream pipeline events via Server-Sent Events (SSE) so the frontend
    can show real-time progress."""

    async def event_generator() -> AsyncGenerator[dict, None]:
        initial_state: PipelineState = {
            "competitor": req.competitor,
            "monitor_urls": req.urls or [],
            "previous_hashes": {},
            "changes_detected": [],
            "research_results": [],
            "comparison_matrix": {},
            "battlecard": {},
            "alerts_sent": [],
            "quality_score": 0.0,
            "reflexion_count": 0,
            "error": None,
        }

        try:
            async for event in pipeline.astream(initial_state):
                for node_name, node_output in event.items():
                    yield {
                        "event": node_name,
                        "data": json.dumps(node_output, default=str, ensure_ascii=False),
                    }
        except Exception as exc:
            yield {
                "event": "error",
                "data": json.dumps({"error": str(exc)}),
            }

    return EventSourceResponse(event_generator())


@app.get("/competitors")
async def list_competitors():
    """Return a list of pre-configured competitors (demo endpoint)."""
    return {
        "competitors": [
            {"name": "CompetitorA", "website": "https://competitora.com"},
            {"name": "CompetitorB", "website": "https://competitorb.com"},
            {"name": "CompetitorC", "website": "https://competitorc.com"},
        ]
    }
