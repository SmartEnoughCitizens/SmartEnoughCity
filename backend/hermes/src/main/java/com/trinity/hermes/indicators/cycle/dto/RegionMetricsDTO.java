package com.trinity.hermes.indicators.cycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionMetricsDTO {

  private String regionId;
  private Long stationCount;
  private Long totalCapacity;
  private Double avgUsageRate;
  private Double avgAvailableBikes;
  private Double avgAvailableDocks;
  private Long emptyStations;
  private Long fullStations;
}
