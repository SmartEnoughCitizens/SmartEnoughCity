package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusNewStopRecommendationDTO {

  private String routeId;
  private String routeShortName;
  private String routeLongName;
  private BusStopSummaryDTO stopA;
  private BusStopSummaryDTO stopB;
  private Double candidateLat;
  private Double candidateLon;
  private Double populationScore;
  private Double publicSpaceScore;
  private Double combinedScore;
}
