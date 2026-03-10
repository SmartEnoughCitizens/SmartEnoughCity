package com.trinity.hermes.indicators.cycle.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationEventDTO {

  private Integer stationId;
  private String stationName;
  private Instant eventTime;
  private Integer availableBikes;
  private Integer prevAvailableBikes;
  private String eventType;
}
