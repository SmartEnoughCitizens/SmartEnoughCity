package com.trinity.hermes.indicators.cycle.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationTimeSeriesDTO {

  private Instant period;
  private Double avgAvailableBikes;
  private Double avgAvailableDocks;
  private Double usageRatePct;
}
