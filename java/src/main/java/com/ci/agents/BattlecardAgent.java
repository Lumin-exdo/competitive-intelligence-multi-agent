package com.ci.agents;

import com.ci.model.Battlecard;
import com.ci.model.PipelineState;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BattlecardAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Sales Battlecard Generator.
            Create an actionable battlecard for sales reps.
            
            Return JSON: {our_strengths:[str], our_weaknesses:[str],
            competitor_strengths:[str], competitor_weaknesses:[str],
            key_differentiators:[str], objection_handling:{str:str},
            elevator_pitch:str}.
            """;

    @SuppressWarnings("unchecked")
    public PipelineState execute(PipelineState state) {
        ChatLanguageModel llm = buildLLM();

        try {
            String comparisonJson = objectMapper.writeValueAsString(state.getComparisonMatrix());
            String researchJson = objectMapper.writeValueAsString(state.getResearchResults());

            String userMsg = """
                    Competitor: %s
                    
                    Comparison Matrix:
                    %s
                    
                    Research Insights:
                    %s
                    
                    Generate a battlecard as JSON.
                    """.formatted(state.getCompetitor(), comparisonJson, researchJson);

            String response = llm.generate(SYSTEM_PROMPT + "\n\n" + userMsg);
            Map<String, Object> data = parseJson(response, new TypeReference<>() {});

            if (data != null) {
                state.setBattlecard(Battlecard.builder()
                        .competitor(state.getCompetitor())
                        .ourStrengths(toStringList(data.get("our_strengths")))
                        .ourWeaknesses(toStringList(data.get("our_weaknesses")))
                        .competitorStrengths(toStringList(data.get("competitor_strengths")))
                        .competitorWeaknesses(toStringList(data.get("competitor_weaknesses")))
                        .keyDifferentiators(toStringList(data.get("key_differentiators")))
                        .objectionHandling(data.get("objection_handling") instanceof Map<?, ?> m
                                ? (Map<String, String>) (Map<?, ?>) m : Map.of())
                        .elevatorPitch((String) data.getOrDefault("elevator_pitch", ""))
                        .build());
            }
        } catch (Exception e) {
            log.error("Battlecard agent failed", e);
        }

        return state;
    }

    private List<String> toStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
