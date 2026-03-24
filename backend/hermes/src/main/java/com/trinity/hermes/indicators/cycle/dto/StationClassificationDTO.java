package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classifies a station by the time-of-day period in which it sees peak demand.
 *
 * <p>Classifications:
 * <ul>
 *   <li>MORNING_PEAK — peak hour falls in 06:00–09:59
 *   <li>LUNCH_PEAK   — peak hour falls in 10:00–14:59
 *   <li>EVENING_PEAK — peak hour falls in 15:00–20:59
 *   <li>OFF_PEAK     — peak hour falls outside the above windows
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationClassificationDTO {

  private Integer stationId;
  private String name;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private String regionId;

  /** Time-of-day classification based on peak hour. */
  private String classification;

  /** Hour of day (0–23) with the highest average usage rate. */
  private Integer peakHour;

  /** Average usage rate (%) during the peak hour. */
  private Double peakUsageRate;
}
