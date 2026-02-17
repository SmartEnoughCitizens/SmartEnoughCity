package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusSystemPerformanceDTO {

  private double reliabilityPct;
  private double evAdoptionPct;
  private double lateArrivalPct;
}
