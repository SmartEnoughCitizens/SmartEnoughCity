package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One scheduled stop on the representative trip (sequence order). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteStopDTO {

  private int sequence;
  private String stopId;
  private Integer code;
  private String name;
  private Double lat;
  private Double lon;
  private String headsign;
}
