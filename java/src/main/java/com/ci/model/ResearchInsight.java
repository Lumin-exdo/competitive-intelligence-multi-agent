package com.ci.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchInsight {
    private String topic;
    private String summary;
    @Builder.Default
    private List<String> keyFindings = List.of();
    @Builder.Default
    private List<String> sources = List.of();
    @Builder.Default
    private double confidence = 0.8;
}
