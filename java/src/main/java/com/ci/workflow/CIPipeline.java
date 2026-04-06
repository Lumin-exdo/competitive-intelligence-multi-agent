package com.ci.workflow;

import com.ci.agents.*;
import com.ci.model.PipelineState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sequential pipeline orchestrator that mirrors the LangGraph workflow
 * in the Python version.
 *
 * <pre>
 * Monitor → Alert (async)
 *         → Research → Compare → Battlecard → Quality Check
 *                                                 ↓
 *                                          score < 7 → retry Research
 * </pre>
 */
@Service
public class CIPipeline {

    private static final Logger log = LoggerFactory.getLogger(CIPipeline.class);

    private final MonitorAgent monitorAgent;
    private final ResearchAgent researchAgent;
    private final CompareAgent compareAgent;
    private final BattlecardAgent battlecardAgent;
    private final AlertAgent alertAgent;
    private final QualityChecker qualityChecker;

    @Value("${ci.pipeline.quality-threshold:7.0}")
    private double qualityThreshold;

    @Value("${ci.pipeline.max-retries:3}")
    private int maxRetries;

    public CIPipeline(MonitorAgent monitorAgent,
                      ResearchAgent researchAgent,
                      CompareAgent compareAgent,
                      BattlecardAgent battlecardAgent,
                      AlertAgent alertAgent,
                      QualityChecker qualityChecker) {
        this.monitorAgent = monitorAgent;
        this.researchAgent = researchAgent;
        this.compareAgent = compareAgent;
        this.battlecardAgent = battlecardAgent;
        this.alertAgent = alertAgent;
        this.qualityChecker = qualityChecker;
    }

    public PipelineState run(String competitor) {
        PipelineState state = PipelineState.builder()
                .competitor(competitor)
                .build();

        log.info("▶ Pipeline started for: {}", competitor);

        state = monitorAgent.execute(state);
        log.info("  ✓ Monitor: {} changes detected", state.getChangesDetected().size());

        // Alert runs independently on detected changes
        state = alertAgent.execute(state);
        log.info("  ✓ Alert: {} alerts sent", state.getAlertsSent().size());

        // Research → Compare → Battlecard loop with Reflexion
        do {
            state = researchAgent.execute(state);
            log.info("  ✓ Research: {} insights", state.getResearchResults().size());

            state = compareAgent.execute(state);
            log.info("  ✓ Compare: matrix generated");

            state = battlecardAgent.execute(state);
            log.info("  ✓ Battlecard: generated");

            double score = qualityChecker.evaluate(state);
            state.setQualityScore(score);
            state.setReflexionCount(state.getReflexionCount() + 1);
            log.info("  ✓ Quality: {}/10 (attempt {}/{})",
                    score, state.getReflexionCount(), maxRetries);

        } while (state.getQualityScore() < qualityThreshold
                && state.getReflexionCount() < maxRetries);

        log.info("▶ Pipeline completed for: {} (quality: {})",
                competitor, state.getQualityScore());
        return state;
    }
}
