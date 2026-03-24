package com.trinity.hermes.indicators.car.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateCarEmissionsId implements Serializable {

  private Integer year;
  private String emissionBand;
  private String licensingAuthority;
}
