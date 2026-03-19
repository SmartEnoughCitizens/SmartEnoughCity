package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationODPairDTO {

  private Integer originStationId;
  private String originName;
  private BigDecimal originLat;
  private BigDecimal originLon;

  private Integer destStationId;
  private String destName;
  private BigDecimal destLat;
  private BigDecimal destLon;

  private Long estimatedTrips;
}
