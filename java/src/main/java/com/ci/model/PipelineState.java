package com.ci.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable state object that flows through the sequential pipeline,
 * analogous to LangGraph's PipelineState in the Python version.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineState {
    private String competitor;
    @Builder.Default
    private List<String> monitorUrls = new ArrayList<>();
    @Builder.Default
    private List<CompetitorChange> changesDetected = new ArrayList<>();
    @Builder.Default
    private List<ResearchInsight> researchResults = new ArrayList<>();
    private ComparisonMatrix comparisonMatrix;
    private Battlecard battlecard;
    @Builder.Default
    private List<String> alertsSent = new ArrayList<>();
    @Builder.Default
    private double qualityScore = 0.0;
    @Builder.Default
    private int reflexionCount = 0;
}
