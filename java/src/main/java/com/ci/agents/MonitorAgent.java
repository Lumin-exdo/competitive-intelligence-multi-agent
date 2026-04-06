package com.ci.agents;

import com.ci.model.ChangeType;
import com.ci.model.CompetitorChange;
import com.ci.model.PipelineState;
import com.ci.model.Severity;
import com.ci.tools.WebScraper;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MonitorAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Competitive Intelligence Monitor Agent.
            Analyze the web page content and detect meaningful changes:
            - Pricing changes
            - New product features or launches
            - Hiring signals
            - Important news
            
            Return a JSON array of objects with keys:
            change_type (PRICING|PRODUCT|HIRING|NEWS), title, summary,
            severity (LOW|MEDIUM|HIGH|CRITICAL), url.
            If no changes, return [].
            """;

    private final WebScraper scraper;

    public MonitorAgent(WebScraper scraper) {
        this.scraper = scraper;
    }

    public PipelineState execute(PipelineState state) {
        ChatLanguageModel llm = buildLLM();
        List<CompetitorChange> allChanges = new ArrayList<>(state.getChangesDetected());

        List<String> urls = state.getMonitorUrls();
        if (urls == null || urls.isEmpty()) {
            urls = defaultUrls(state.getCompetitor());
        }

        for (String url : urls) {
            try {
                String content = scraper.fetchAndExtractText(url);
                if (content == null || content.isBlank()) continue;

                String truncated = content.length() > 6000
                        ? content.substring(0, 6000) : content;

                String userMsg = "Competitor: %s\nURL: %s\n\nContent:\n%s\n\nReturn changes as JSON."
                        .formatted(state.getCompetitor(), url, truncated);

                String response = llm.generate(SYSTEM_PROMPT + "\n\n" + userMsg);
                List<Map<String, Object>> items = parseJson(response,
                        new TypeReference<>() {});

                if (items != null) {
                    for (var item : items) {
                        allChanges.add(CompetitorChange.builder()
                                .competitor(state.getCompetitor())
                                .changeType(ChangeType.valueOf(
                                        ((String) item.getOrDefault("change_type", "NEWS")).toUpperCase()))
                                .title((String) item.getOrDefault("title", ""))
                                .summary((String) item.getOrDefault("summary", ""))
                                .url((String) item.getOrDefault("url", url))
                                .severity(Severity.valueOf(
                                        ((String) item.getOrDefault("severity", "MEDIUM")).toUpperCase()))
                                .build());
                    }
                }
            } catch (Exception e) {
                log.error("Error monitoring {}", url, e);
            }
        }

        state.setChangesDetected(allChanges);
        return state;
    }

    private List<String> defaultUrls(String competitor) {
        String slug = competitor.toLowerCase().replaceAll("\\s+", "");
        return List.of(
                "https://" + slug + ".com",
                "https://" + slug + ".com/pricing",
                "https://" + slug + ".com/careers"
        );
    }
}
