package com.trinity.hermes.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecommendationRequest {
    private String name;
    private String description;
    private String status;
}
