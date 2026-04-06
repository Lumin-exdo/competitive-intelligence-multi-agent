package com.ci.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Battlecard {
    private String competitor;
    @Builder.Default
    private List<String> ourStrengths = List.of();
    @Builder.Default
    private List<String> ourWeaknesses = List.of();
    @Builder.Default
    private List<String> competitorStrengths = List.of();
    @Builder.Default
    private List<String> competitorWeaknesses = List.of();
    @Builder.Default
    private List<String> keyDifferentiators = List.of();
    @Builder.Default
    private Map<String, String> objectionHandling = Map.of();
    private String elevatorPitch;
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
