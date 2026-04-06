package com.ci.agents;

import com.ci.model.PipelineState;
import com.ci.model.ResearchInsight;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ResearchAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Competitive Intelligence Research Agent.
            Perform deep-dive analysis covering:
            1. Financial signals  2. Patent activity  3. Tech blogs
            4. Open-source contributions  5. Strategic moves
            
            Return a JSON array: [{topic, summary, key_findings:[str], sources:[str], confidence:float}].
            """;

    public PipelineState execute(PipelineState state) {
        ChatLanguageModel llm = buildLLM();

        String changesSummary = state.getChangesDetected().stream()
                .map(c -> "- [%s] %s: %s".formatted(c.getChangeType(), c.getTitle(), c.getSummary()))
                .reduce("", (a, b) -> a + "\n" + b);

        if (changesSummary.isBlank()) {
            changesSummary = "No specific changes – perform general research.";
        }

        String userMsg = "Competitor: %s\n\nDetected Changes:\n%s\n\nProvide research insights as JSON."
                .formatted(state.getCompetitor(), changesSummary);

        String response = llm.generate(SYSTEM_PROMPT + "\n\n" + userMsg);

        List<Map<String, Object>> items = parseJson(response, new TypeReference<>() {});
        List<ResearchInsight> insights = new ArrayList<>();

        if (items != null) {
            for (var item : items) {
                insights.add(ResearchInsight.builder()
                        .topic((String) item.getOrDefault("topic", "General"))
                        .summary((String) item.getOrDefault("summary", ""))
                        .keyFindings(toStringList(item.get("key_findings")))
                        .sources(toStringList(item.get("sources")))
                        .confidence(toDouble(item.get("confidence"), 0.7))
                        .build());
            }
        }

        state.setResearchResults(insights);
        return state;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private double toDouble(Object obj, double fallback) {
        if (obj instanceof Number n) return n.doubleValue();
        return fallback;
    }
}
