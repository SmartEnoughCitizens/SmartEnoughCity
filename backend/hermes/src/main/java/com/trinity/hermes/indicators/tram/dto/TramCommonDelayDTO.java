package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramCommonDelayDTO {

  private String stopId;
  private String stopName;
  private String line;
  private double avgDelayMins;
  private int maxDelayMins;
  private long delayCount;
  private Double lat;
  private Double lon;
}
