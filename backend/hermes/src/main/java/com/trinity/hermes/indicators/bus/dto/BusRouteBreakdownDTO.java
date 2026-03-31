package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteBreakdownDTO {
  private String stopId;
  private Double avgDelayMinutes;
  private Double maxDelayMinutes;
  private Long tripCount;
}
