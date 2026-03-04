package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusRouteUtilizationDTO {

  private String routeId;
  private String routeShortName;
  private String routeLongName;
  private double utilizationPct;
  private int activeVehicles;
  private String status;
}
