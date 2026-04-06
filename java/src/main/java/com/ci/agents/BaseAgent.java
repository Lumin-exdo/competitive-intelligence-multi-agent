package com.ci.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Base class providing shared LLM access for all agents.
 */
public abstract class BaseAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ci.llm.api-key:}")
    private String apiKey;

    @Value("${ci.llm.model:gpt-4o}")
    private String model;

    @Value("${ci.llm.temperature:0.3}")
    private double temperature;

    protected ChatLanguageModel buildLLM() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();
    }

    /**
     * Utility: strip markdown fences and parse JSON from LLM output.
     */
    protected <T> T parseJson(String raw, TypeReference<T> ref) {
        try {
            String text = raw.strip();
            if (text.startsWith("```")) {
                text = text.substring(text.indexOf('\n') + 1);
                int end = text.lastIndexOf("```");
                if (end > 0) text = text.substring(0, end);
            }
            return objectMapper.readValue(text, ref);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON output: {}", e.getMessage());
            return null;
        }
    }
}
