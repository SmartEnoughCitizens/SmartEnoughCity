package com.trinity.hermes.indicators.car.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarDashboardDTO {

  private String fuelType;
  private Long count;
}
