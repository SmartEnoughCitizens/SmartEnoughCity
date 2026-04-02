package com.trinity.hermes.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecommendationRequest {
  private String indicator;
  private String recommendation;
  private String usecase;
  private String simulation;
  private Boolean deleted;
  private String status;
}
