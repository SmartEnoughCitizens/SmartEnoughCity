package com.trinity.hermes.indicators.cycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationClassificationDTO {

  private int stationId;
  private String name;
  private int peakHour; // 0–23, the hour with highest avg usage rate
  private double peakUsage; // avg usage rate at peak hour (0–100)
  private String classification; // MORNING_PEAK | AFTERNOON_PEAK | EVENING_PEAK | OFF_PEAK
}
