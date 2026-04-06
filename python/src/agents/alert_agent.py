"""Alert Agent – real-time critical-change notifications via Slack / DingTalk / Email."""

from __future__ import annotations

import logging
from datetime import datetime
from typing import Any

from ..models.schemas import Alert, CompetitorChange, Severity
from ..tools.notification import broadcast_alert

logger = logging.getLogger(__name__)

SEVERITY_THRESHOLD = Severity.HIGH


class AlertAgent:
    """Runs independently: evaluates changes and pushes alerts for high/critical
    severity items."""

    async def evaluate_and_alert(
        self,
        changes: list[CompetitorChange],
    ) -> list[Alert]:
        alerts: list[Alert] = []

        critical_changes = [
            c for c in changes
            if c.severity in (Severity.HIGH, Severity.CRITICAL)
        ]

        for change in critical_changes:
            title = f"[{change.severity.value.upper()}] {change.competitor}: {change.title}"
            message = (
                f"Change Type: {change.change_type.value}\n"
                f"Summary: {change.summary}\n"
                f"URL: {change.url}\n"
                f"Detected: {change.detected_at.isoformat()}"
            )

            delivery = await broadcast_alert(title, message)

            alert = Alert(
                competitor=change.competitor,
                title=title,
                message=message,
                severity=change.severity,
                channel=", ".join(ch for ch, ok in delivery.items() if ok) or "none",
                sent_at=datetime.utcnow(),
            )
            alerts.append(alert)
            logger.info("Alert sent: %s (channels: %s)", title, alert.channel)

        return alerts

    # ------------------------------------------------------------------
    # LangGraph node
    # ------------------------------------------------------------------

    async def __call__(self, state: dict[str, Any]) -> dict[str, Any]:
        raw_changes = state.get("changes_detected", [])
        changes = [
            CompetitorChange(**c) if isinstance(c, dict) else c
            for c in raw_changes
        ]

        alerts = await self.evaluate_and_alert(changes)
        return {
            "alerts_sent": [a.model_dump() for a in alerts],
        }
