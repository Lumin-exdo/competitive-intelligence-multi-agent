"""Web scraping utilities for monitoring competitor websites."""

from __future__ import annotations

import hashlib
import logging
from typing import Optional

import httpx
from bs4 import BeautifulSoup
from tenacity import retry, stop_after_attempt, wait_exponential

logger = logging.getLogger(__name__)

_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/131.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "en-US,en;q=0.9,zh-CN;q=0.8",
}


def content_hash(text: str) -> str:
    """Return a SHA-256 hex digest used for change detection."""
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


@retry(stop=stop_after_attempt(3), wait=wait_exponential(min=2, max=30))
async def fetch_page(url: str, timeout: float = 30.0) -> Optional[str]:
    """Fetch a page and return its HTML content."""
    async with httpx.AsyncClient(headers=_HEADERS, follow_redirects=True) as client:
        resp = await client.get(url, timeout=timeout)
        resp.raise_for_status()
        return resp.text


def extract_text(html: str) -> str:
    """Strip tags and return visible text."""
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup(["script", "style", "noscript"]):
        tag.decompose()
    return soup.get_text(separator="\n", strip=True)


def extract_pricing(html: str) -> list[dict]:
    """Heuristic extraction of pricing information from a page.

    Returns a list of ``{"plan": ..., "price": ..., "features": [...]}`` dicts.
    The implementation uses simple CSS-selector heuristics; production systems
    would combine this with an LLM extraction step.
    """
    soup = BeautifulSoup(html, "html.parser")
    plans: list[dict] = []

    pricing_sections = soup.select(
        "[class*=pricing], [class*=plan], [id*=pricing], [id*=plan]"
    )
    for section in pricing_sections:
        title_el = section.select_one("h2, h3, h4, [class*=title], [class*=name]")
        price_el = section.select_one("[class*=price], [class*=amount], .price")
        features = [li.get_text(strip=True) for li in section.select("li")]

        plans.append({
            "plan": title_el.get_text(strip=True) if title_el else "Unknown",
            "price": price_el.get_text(strip=True) if price_el else "N/A",
            "features": features[:10],
        })

    return plans


def extract_job_listings(html: str) -> list[dict]:
    """Best-effort extraction of job postings from a careers page."""
    soup = BeautifulSoup(html, "html.parser")
    jobs: list[dict] = []

    job_cards = soup.select(
        "[class*=job], [class*=position], [class*=opening], [class*=career]"
    )
    for card in job_cards[:20]:
        title_el = card.select_one("h2, h3, h4, a, [class*=title]")
        location_el = card.select_one("[class*=location], [class*=place]")
        jobs.append({
            "title": title_el.get_text(strip=True) if title_el else "",
            "location": location_el.get_text(strip=True) if location_el else "",
        })

    return [j for j in jobs if j["title"]]
