package com.trinity.hermes.indicators.train.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainKpiDTO {
  private long totalStations;
  private long liveTrainsRunning;
  private double onTimePct;
  private double avgDelayMinutes;
}
