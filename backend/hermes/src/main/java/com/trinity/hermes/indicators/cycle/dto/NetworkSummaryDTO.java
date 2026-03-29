package com.trinity.hermes.indicators.cycle.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkSummaryDTO {

  private Integer totalStations;
  private Integer activeStations;
  private Integer totalBikesAvailable;
  private Integer totalDocksAvailable;
  private Integer totalDisabledBikes;
  private Integer totalDisabledDocks;
  private Integer emptyStations;
  private Integer fullStations;
  private Double avgNetworkFullnessPct;
  private Integer rebalancingNeedCount;
  private Instant dataAsOf;
}
