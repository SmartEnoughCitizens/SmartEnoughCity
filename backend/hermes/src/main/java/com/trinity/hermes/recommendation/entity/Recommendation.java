package com.trinity.hermes.recommendation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recommendation entity - simplified for thin slice.
 * Not using JPA @Entity for now to avoid database setup complexity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String dataIndicator;
    private String notificationSent;
    private String createdAt;
    private String completedAt;
    private RecommendationDetails recommendationData;
}