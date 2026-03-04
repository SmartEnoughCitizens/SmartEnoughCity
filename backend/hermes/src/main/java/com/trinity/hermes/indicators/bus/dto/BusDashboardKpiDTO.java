package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusDashboardKpiDTO {

  private long totalBusesRunning;
  private long activeDelays;
  private double fleetUtilizationPct;
  private double sustainabilityScore;
}
