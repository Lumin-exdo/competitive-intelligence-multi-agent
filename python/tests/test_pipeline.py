"""Smoke tests for the CI pipeline (runs without external services)."""

import pytest
from src.models.schemas import (
    Battlecard,
    ChangeType,
    ComparisonMatrix,
    CompetitorChange,
    DimensionScore,
    ResearchInsight,
    Severity,
)


def test_competitor_change_model():
    change = CompetitorChange(
        competitor="Acme",
        change_type=ChangeType.PRICING,
        title="Price increase",
        summary="Pro plan went from $49 to $59/mo",
        severity=Severity.HIGH,
    )
    assert change.competitor == "Acme"
    assert change.severity == Severity.HIGH


def test_comparison_matrix_model():
    matrix = ComparisonMatrix(
        competitor="Acme",
        dimensions=[
            DimensionScore(dimension="Pricing", our_score=8.0, competitor_score=6.0),
        ],
        overall_assessment="We lead on pricing.",
    )
    assert len(matrix.dimensions) == 1
    assert matrix.dimensions[0].our_score > matrix.dimensions[0].competitor_score


def test_battlecard_model():
    card = Battlecard(
        competitor="Acme",
        our_strengths=["Better pricing", "Faster support"],
        competitor_weaknesses=["Slow onboarding"],
        elevator_pitch="We deliver more value at a lower price.",
    )
    assert len(card.our_strengths) == 2
    assert card.elevator_pitch


def test_research_insight_confidence_bounds():
    with pytest.raises(Exception):
        ResearchInsight(topic="Test", summary="x", confidence=1.5)
