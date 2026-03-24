package com.trinity.hermes.indicators.car.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "private_car_emissions", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(PrivateCarEmissionsId.class)
public class PrivateCarEmissions {

  @Id
  @Column(name = "year", nullable = false)
  private Integer year;

  @Id
  @Column(name = "emission_band", nullable = false)
  private String emissionBand;

  @Id
  @Column(name = "licensing_authority", nullable = false)
  private String licensingAuthority;

  @Column(name = "count", nullable = false)
  private Long count;
}
