package com.trinity.hermes.recommendation.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationEngineRequest {

  private String indicatorType;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private Integer limit;
  private String aggregationType;
}
