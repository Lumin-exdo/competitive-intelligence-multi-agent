package com.ci.workflow;

import com.ci.agents.BaseAgent;
import com.ci.model.PipelineState;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QualityChecker extends BaseAgent {

    public double evaluate(PipelineState state) {
        try {
            ChatLanguageModel llm = buildLLM();
            String battlecardJson = objectMapper.writeValueAsString(state.getBattlecard());

            String prompt = """
                    Rate this battlecard 1-10 on completeness, accuracy, and actionability.
                    Return ONLY JSON: {"score": <float>, "feedback": "<string>"}
                    
                    Battlecard:
                    %s
                    """.formatted(battlecardJson);

            String response = llm.generate(prompt);
            Map<String, Object> result = parseJson(response, new TypeReference<>() {});

            if (result != null && result.containsKey("score")) {
                return ((Number) result.get("score")).doubleValue();
            }
        } catch (Exception e) {
            log.error("Quality check failed", e);
        }
        return 5.0;
    }
}
