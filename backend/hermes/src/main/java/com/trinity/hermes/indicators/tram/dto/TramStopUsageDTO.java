package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramStopUsageDTO {

  private String stopId;
  private String stopName;
  private String line;
  private int currentHour;
  private int inboundTrips;
  private int outboundTrips;
  private int totalTrips;
  private long estimatedInboundPassengers;
  private long estimatedOutboundPassengers;
  private long estimatedTotalPassengers;
  private Double lat;
  private Double lon;
}
