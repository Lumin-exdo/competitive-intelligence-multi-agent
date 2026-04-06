package com.ci.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionScore {
    private String dimension;
    private double ourScore;
    private double competitorScore;
    @Builder.Default
    private String notes = "";
}
