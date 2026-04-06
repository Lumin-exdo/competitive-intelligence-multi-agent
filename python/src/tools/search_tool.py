"""Search tool wrappers for retrieving competitor intelligence from the web."""

from __future__ import annotations

import logging
from typing import Optional

import httpx
from tenacity import retry, stop_after_attempt, wait_exponential

logger = logging.getLogger(__name__)


@retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=15))
async def web_search(query: str, num_results: int = 10, api_key: str = "") -> list[dict]:
    """Search the web via SerpAPI-compatible endpoint.

    Returns a list of ``{"title": ..., "link": ..., "snippet": ...}`` dicts.
    Falls back to a stub when no API key is configured so the project still
    runs in demo mode.
    """
    if not api_key:
        logger.warning("No search API key configured – returning demo results")
        return _demo_results(query)

    async with httpx.AsyncClient() as client:
        resp = await client.get(
            "https://serpapi.com/search",
            params={
                "q": query,
                "api_key": api_key,
                "engine": "google",
                "num": num_results,
            },
            timeout=30.0,
        )
        resp.raise_for_status()
        data = resp.json()

    return [
        {
            "title": r.get("title", ""),
            "link": r.get("link", ""),
            "snippet": r.get("snippet", ""),
        }
        for r in data.get("organic_results", [])
    ]


@retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=15))
async def news_search(query: str, num_results: int = 10, api_key: str = "") -> list[dict]:
    """Search recent news articles about a competitor."""
    if not api_key:
        return _demo_news(query)

    async with httpx.AsyncClient() as client:
        resp = await client.get(
            "https://serpapi.com/search",
            params={
                "q": query,
                "api_key": api_key,
                "engine": "google_news",
                "num": num_results,
            },
            timeout=30.0,
        )
        resp.raise_for_status()
        data = resp.json()

    return [
        {
            "title": r.get("title", ""),
            "link": r.get("link", ""),
            "date": r.get("date", ""),
            "source": r.get("source", {}).get("name", ""),
        }
        for r in data.get("news_results", [])
    ]


async def job_search(company: str, api_key: str = "") -> list[dict]:
    """Search for recent job postings of a given company."""
    return await web_search(f"{company} careers hiring jobs 2026", api_key=api_key)


# ---------------------------------------------------------------------------
# Demo / fallback stubs (allows running without API keys)
# ---------------------------------------------------------------------------

def _demo_results(query: str) -> list[dict]:
    return [
        {
            "title": f"[Demo] {query} – Official Website",
            "link": "https://example.com",
            "snippet": f"Demo search result for: {query}. Configure SERPAPI_KEY for real results.",
        },
    ]


def _demo_news(query: str) -> list[dict]:
    return [
        {
            "title": f"[Demo] {query} Announces New Product",
            "link": "https://example.com/news",
            "date": "2026-04-01",
            "source": "Demo News",
        },
    ]
