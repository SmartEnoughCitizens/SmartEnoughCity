package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-station hourly demand profile: average usage rate for each hour of day (0–23). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationHourlyProfileDTO {

  private Integer stationId;
  private String name;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private Integer capacity;
  private String regionId;

  /** Map of hour (0–23) → avg usage rate (%). Only hours with data are present. */
  private Map<Integer, Double> hourlyRates;
}
