package com.trinity.hermes.recommendation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDetails {
    private String transportMode;
    private String routes;
    private String estimatedTime;
    private String alternatives;
    private String confidenceScore;
    private String generatedAt;
}
