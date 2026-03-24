package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramKpiDTO {

  private long totalStops;
  private long activeForecastCount;
  private long linesOperating;
  private double avgDueMins;
}
