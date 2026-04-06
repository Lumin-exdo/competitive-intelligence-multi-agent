package com.ci.agents;

import com.ci.model.CompetitorChange;
import com.ci.model.PipelineState;
import com.ci.model.Severity;
import com.ci.tools.NotificationService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AlertAgent extends BaseAgent {

    private final NotificationService notificationService;

    public AlertAgent(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public PipelineState execute(PipelineState state) {
        List<String> alerts = new ArrayList<>(state.getAlertsSent());

        List<CompetitorChange> criticalChanges = state.getChangesDetected().stream()
                .filter(c -> c.getSeverity() == Severity.HIGH || c.getSeverity() == Severity.CRITICAL)
                .toList();

        for (CompetitorChange change : criticalChanges) {
            String title = "[%s] %s: %s".formatted(
                    change.getSeverity(), change.getCompetitor(), change.getTitle());
            String message = """
                    Change Type: %s
                    Summary: %s
                    URL: %s
                    """.formatted(change.getChangeType(), change.getSummary(), change.getUrl());

            boolean sent = notificationService.broadcast(title, message);
            alerts.add(title + (sent ? " [SENT]" : " [FAILED]"));
            log.info("Alert: {} -> {}", title, sent ? "sent" : "failed");
        }

        state.setAlertsSent(alerts);
        return state;
    }
}
