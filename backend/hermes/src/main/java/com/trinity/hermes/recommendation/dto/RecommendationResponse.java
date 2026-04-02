package com.trinity.hermes.recommendation.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
  private Integer id;
  private String indicator;
  private String recommendation;
  private String usecase;
  private String simulation;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean deleted;
  private String status;
}
