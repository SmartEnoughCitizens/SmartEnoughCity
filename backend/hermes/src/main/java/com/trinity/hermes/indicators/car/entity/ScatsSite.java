package com.trinity.hermes.indicators.car.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "scats_sites", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScatsSite {

  @Id
  @Column(name = "site_id", nullable = false)
  private Integer siteId;

  @Column(nullable = false)
  private Double lat;

  @Column(nullable = false)
  private Double lon;
}
