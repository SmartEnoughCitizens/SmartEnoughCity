package com.trinity.hermes.indicators.car.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarStatisticsId implements Serializable {

  private Integer year;
  private String taxationClass;
  private String fuelType;
}
