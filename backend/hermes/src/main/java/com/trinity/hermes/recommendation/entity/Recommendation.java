package com.trinity.hermes.Recommendation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.Embeddable;
import javax.persistence.Entity;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Recommendation {
    private Long id;
    private String dataIndicator;
    private String notificationSent;
    private String status;
    private String createdAt;
    private String completedAt;
    private RecommendationDetails recommendationData;
}

@Embeddable
public class RecommendationDetails {
    private String transportMode;
    private String routes;
    private String estimatedTime;
    private String alternatives;
    private String confidenceScore;
    private String generatedAt;
}