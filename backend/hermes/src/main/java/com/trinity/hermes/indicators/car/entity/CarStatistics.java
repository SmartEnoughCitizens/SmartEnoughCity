package com.trinity.hermes.indicators.car.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vehicle_yearly", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CarStatisticsId.class)
public class CarStatistics {

  @Id
  @Column(name = "year", nullable = false)
  private Integer year;

  @Id
  @Column(name = "taxation_class", nullable = false)
  private String taxationClass;

  @Id
  @Column(name = "fuel_type", nullable = false)
  private String fuelType;

  @Column(name = "count", nullable = false)
  private Long count;
}
