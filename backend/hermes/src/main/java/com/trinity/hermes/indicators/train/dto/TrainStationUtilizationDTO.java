package com.trinity.hermes.indicators.train.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-station utilization snapshot.
 *
 * <p>{@code trainServiceCount} = number of active train services currently tracked at this station
 * (latest snapshot). {@code utilizationLevel} is one of {@code "HIGH"}, {@code "MEDIUM"}, or {@code
 * "LOW"} relative to the Dublin-wide mean.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainStationUtilizationDTO {

  private String stationCode;
  private String stationDesc;
  private double lat;
  private double lon;

  /** Number of distinct train services at this station in the latest data snapshot. */
  private long trainServiceCount;

  /** Average delay in minutes across active services at this station. */
  private double avgDelayMinutes;

  /**
   * Relative utilization band compared to the Dublin-wide average: {@code "HIGH"} (&gt;150 % of
   * mean), {@code "MEDIUM"} (50–150 %), {@code "LOW"} (&lt;50 %).
   */
  private String utilizationLevel;
}
