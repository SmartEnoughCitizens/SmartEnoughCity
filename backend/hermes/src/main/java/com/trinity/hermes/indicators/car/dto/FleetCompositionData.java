package com.trinity.hermes.indicators.car.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FleetCompositionData {

  private long totalFleet;
  private double evPercent;
  private double icePercent;

  /** CO2 emission band share of the private-car fleet, keyed by band (e.g. "A", "B", …, "G"). */
  private Map<String, Double> bandPercents;
}
