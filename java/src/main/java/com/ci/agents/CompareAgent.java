package com.ci.agents;

import com.ci.model.ComparisonMatrix;
import com.ci.model.DimensionScore;
import com.ci.model.PipelineState;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CompareAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Comparison Agent. Score both our product and the competitor (0-10)
            across: Product Features, Pricing, UX, Market Share, Customer Sentiment,
            Technology, Ecosystem, Support.
            
            Return JSON: {dimensions:[{dimension, our_score, competitor_score, notes}],
            overall_assessment: string}.
            """;

    public PipelineState execute(PipelineState state) {
        ChatLanguageModel llm = buildLLM();

        try {
            String researchJson = objectMapper.writeValueAsString(state.getResearchResults());
            String userMsg = "Competitor: %s\n\nResearch:\n%s\n\nGenerate comparison matrix as JSON."
                    .formatted(state.getCompetitor(), researchJson);

            String response = llm.generate(SYSTEM_PROMPT + "\n\n" + userMsg);
            Map<String, Object> data = parseJson(response, new TypeReference<>() {});

            if (data != null) {
                List<DimensionScore> dims = new ArrayList<>();
                Object dimsRaw = data.get("dimensions");
                if (dimsRaw instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> m) {
                            dims.add(DimensionScore.builder()
                                    .dimension((String) m.getOrDefault("dimension", ""))
                                    .ourScore(toDouble(m.get("our_score")))
                                    .competitorScore(toDouble(m.get("competitor_score")))
                                    .notes((String) m.getOrDefault("notes", ""))
                                    .build());
                        }
                    }
                }

                state.setComparisonMatrix(ComparisonMatrix.builder()
                        .competitor(state.getCompetitor())
                        .dimensions(dims)
                        .overallAssessment((String) data.getOrDefault("overall_assessment", ""))
                        .build());
            }
        } catch (Exception e) {
            log.error("Compare agent failed", e);
        }

        return state;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        return 5.0;
    }
}
