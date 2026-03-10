package com.trinity.hermes.indicators.car.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HighTrafficPointsDTO {

  private Integer siteId;
  private Double lat;
  private Double lon;
  private Double avgVolume;
  private String dayType;
  private String timeSlot;
}
