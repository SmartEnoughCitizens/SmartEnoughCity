package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusLiveVehicleDTO {

  private Integer vehicleId;
  private String routeShortName;
  private double latitude;
  private double longitude;
  private String status;
  private double occupancyPct;
  private int delaySeconds;
}
