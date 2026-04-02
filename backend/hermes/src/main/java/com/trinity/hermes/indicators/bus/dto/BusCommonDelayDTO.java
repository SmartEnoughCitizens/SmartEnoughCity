package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusCommonDelayDTO {
  private String routeId;
  private String routeShortName;
  private String routeLongName;
  private Double avgDelayMinutes;
}
