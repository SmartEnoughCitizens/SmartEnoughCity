package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramStopDTO {

  private String stopId;
  private String line;
  private String name;
  private Double lat;
  private Double lon;
  private Boolean parkRide;
  private Boolean cycleRide;
}
