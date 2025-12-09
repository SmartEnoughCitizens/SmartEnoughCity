package com.trinity.hermes.recommendation.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Recommendation {
    @Id
    private Long id;

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

@Embeddable
class RecommendationDetails {

}

