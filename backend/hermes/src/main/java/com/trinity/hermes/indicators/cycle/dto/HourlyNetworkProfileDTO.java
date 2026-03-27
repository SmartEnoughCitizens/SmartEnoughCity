package com.trinity.hermes.indicators.cycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyNetworkProfileDTO {

  private int hourOfDay; // 0–23 in Europe/Dublin local time
  private double avgTurnover; // total natural bike movements (ABS delta 1–5) across all stations
  private long stationCount; // number of distinct stations contributing to that hour
}
