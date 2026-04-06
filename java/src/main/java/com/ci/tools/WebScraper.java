package com.ci.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebScraper {

    private static final Logger log = LoggerFactory.getLogger(WebScraper.class);
    private static final int TIMEOUT_MS = 30_000;

    public String fetchAndExtractText(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; CIBot/1.0)")
                    .timeout(TIMEOUT_MS)
                    .get();
            doc.select("script, style, noscript").remove();
            return doc.body().text();
        } catch (Exception e) {
            log.warn("Failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }
}
