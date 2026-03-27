package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Estimated origin → destination trip pair derived from snapshot availability changes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationODPairDTO {

  private int originStationId;
  private String originName;
  private BigDecimal originLat;
  private BigDecimal originLon;

  private int destStationId;
  private String destName;
  private BigDecimal destLat;
  private BigDecimal destLon;

  private int estimatedTrips;
  private double distanceKm;
}
