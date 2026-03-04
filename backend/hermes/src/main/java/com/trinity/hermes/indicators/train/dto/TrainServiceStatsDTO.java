package com.trinity.hermes.indicators.train.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainServiceStatsDTO {
  private double reliabilityPct;
  private double lateArrivalPct;
  private double avgDueMinutes;
}
