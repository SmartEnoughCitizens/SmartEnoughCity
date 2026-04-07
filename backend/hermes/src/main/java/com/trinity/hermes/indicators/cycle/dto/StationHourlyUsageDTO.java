package com.trinity.hermes.indicators.cycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-station, per-hour natural bike turnover — one row per (station, hour) pair. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationHourlyUsageDTO {

  private int stationId;
  private String name;
  private int hourOfDay; // 0–23 Europe/Dublin local time
  private double avgTurnover; // total natural bike movements (ABS delta 1–5) in that hour
}
