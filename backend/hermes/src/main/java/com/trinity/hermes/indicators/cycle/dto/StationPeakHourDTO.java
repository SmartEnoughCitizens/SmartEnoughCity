package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The single busiest hour of day for a station, derived from historical usage rates. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationPeakHourDTO {

  private Integer stationId;
  private String name;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private String regionId;

  /** Hour of day (0–23) with the highest average usage rate for this station. */
  private Integer peakHour;

  /** Average usage rate (%) during the peak hour. */
  private Double peakUsageRate;
}
