package com.trinity.hermes.indicators.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusStopSummaryDTO {

  private String id;
  private Integer code;
  private String name;
  private Double lat;
  private Double lon;
}
