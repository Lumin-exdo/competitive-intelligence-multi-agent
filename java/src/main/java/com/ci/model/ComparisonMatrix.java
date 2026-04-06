package com.ci.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonMatrix {
    private String competitor;
    @Builder.Default
    private List<DimensionScore> dimensions = List.of();
    private String overallAssessment;
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
