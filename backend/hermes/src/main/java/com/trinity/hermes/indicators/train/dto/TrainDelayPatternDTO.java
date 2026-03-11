package com.trinity.hermes.indicators.train.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One recurring delay pattern: a specific station + route (origin→destination) + time-of-day bucket
 * combination that historically shows elevated average delay.
 *
 * <p>{@code timeOfDay} values: {@code MORNING_PEAK} (06–09), {@code MIDDAY} (09–13), {@code
 * AFTERNOON} (13–17), {@code EVENING_PEAK} (17–21), {@code NIGHT} (21–06).
 *
 * <p>{@code severityLevel} values: {@code SEVERE} (avg ≥ 10 min), {@code MODERATE} (5–10 min),
 * {@code MINOR} (1–5 min).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainDelayPatternDTO {

  private String stationCode;
  private String stationDesc;
  private double lat;
  private double lon;

  /** Departure terminal for this service (Irish Rail "origin" field). */
  private String origin;

  /** Arrival terminal for this service (Irish Rail "destination" field). */
  private String destination;

  private String trainType;

  /** One of: MORNING_PEAK, MIDDAY, AFTERNOON, EVENING_PEAK, NIGHT. */
  private String timeOfDay;

  /** Average delay in minutes across all matching historical records. */
  private double avgDelayMinutes;

  /** Worst single recorded delay in minutes for this pattern. */
  private int maxDelayMinutes;

  /** Number of individual train records that make up this pattern. */
  private long occurrenceCount;

  /** Percentage of records where the train was late (lateMinutes &gt; 0). */
  private double latePercent;

  /** SEVERE ≥ 10 min avg · MODERATE 5–10 min · MINOR 1–5 min. */
  private String severityLevel;
}
