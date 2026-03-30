package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramDelayDTO {

  private String stopId;
  private String stopName;
  private String line;
  private String direction;
  private String destination;
  private String scheduledTime;
  private Integer dueMins;
  private Integer delayMins;
  private Double estimatedAffectedPassengers;
}
