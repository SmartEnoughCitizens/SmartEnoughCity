package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceSuggestionDTO {

  /** Full station — no docks available, needs bikes moved out. */
  private Integer sourceStationId;

  private String sourceName;
  private BigDecimal sourceLat;
  private BigDecimal sourceLon;
  private Integer sourceBikes;

  /** Empty station — no bikes available, needs bikes moved in. */
  private Integer targetStationId;

  private String targetName;
  private BigDecimal targetLat;
  private BigDecimal targetLon;
  private Integer targetCapacity;

  /** Straight-line distance in kilometres between source and target. */
  private Double distanceKm;
}
