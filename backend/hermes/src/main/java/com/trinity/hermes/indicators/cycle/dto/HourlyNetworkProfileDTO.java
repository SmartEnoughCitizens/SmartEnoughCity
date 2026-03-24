package com.trinity.hermes.indicators.cycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyNetworkProfileDTO {

  private int hourOfDay;       // 0–23 in Europe/Dublin local time
  private double avgUsageRate; // network average of (capacity - available_docks)/capacity * 100
  private long stationCount;   // number of distinct stations contributing to that hour
}
