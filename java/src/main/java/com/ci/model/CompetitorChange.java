package com.ci.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorChange {
    private String competitor;
    private ChangeType changeType;
    private String title;
    private String summary;
    private String url;
    @Builder.Default
    private Severity severity = Severity.MEDIUM;
    @Builder.Default
    private Instant detectedAt = Instant.now();
}
