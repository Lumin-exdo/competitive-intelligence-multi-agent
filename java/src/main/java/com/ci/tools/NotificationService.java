package com.ci.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final RestTemplate rest = new RestTemplate();

    @Value("${ci.notification.slack-webhook:}")
    private String slackWebhook;

    @Value("${ci.notification.dingtalk-webhook:}")
    private String dingtalkWebhook;

    public boolean broadcast(String title, String message) {
        boolean slackOk = sendSlack(title, message);
        boolean dingOk = sendDingTalk(title, message);
        return slackOk || dingOk;
    }

    private boolean sendSlack(String title, String message) {
        if (slackWebhook == null || slackWebhook.isBlank()) return false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = """
                    {"text": "🚨 *%s*\\n%s"}
                    """.formatted(title, message.replace("\"", "\\\"").replace("\n", "\\n"));
            rest.postForEntity(slackWebhook, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception e) {
            log.warn("Slack notification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendDingTalk(String title, String message) {
        if (dingtalkWebhook == null || dingtalkWebhook.isBlank()) return false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", "🚨 " + title + "\n" + message)
            );
            rest.postForEntity(dingtalkWebhook, new HttpEntity<>(payload, headers), String.class);
            return true;
        } catch (Exception e) {
            log.warn("DingTalk notification failed: {}", e.getMessage());
            return false;
        }
    }
}
