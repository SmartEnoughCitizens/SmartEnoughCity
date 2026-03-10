package com.trinity.hermes.indicators.cycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationRankingDTO {

  private Integer stationId;
  private String name;
  private String regionId;
  private Integer capacity;
  private Double avgUsageRate;
  private Double avgAvailableBikes;
  private Double avgAvailableDocks;
  private Long emptyEventCount;
  private Long fullEventCount;
}
