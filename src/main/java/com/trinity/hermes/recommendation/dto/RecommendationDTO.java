package com.trinity.hermes.Recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
}
