package com.trinity.hermes.recommendation.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private String id;
    private String dataIndicator;
    private String notificationSent;
    private String status;
    private String createdAt;
    private String completedAt;
    private String transportMode;
    private String routes;
    private String estimatedTime;
    private String alternatives;
    private String confidenceScore;
    private String generatedAt;
}

