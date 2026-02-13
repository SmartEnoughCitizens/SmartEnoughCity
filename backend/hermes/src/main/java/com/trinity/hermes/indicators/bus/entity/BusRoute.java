package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_routes", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRoute {

  @Id private String id;

  @Column(name = "agency_id", nullable = false)
  private Integer agencyId;

  @Column(name = "short_name", nullable = false)
  private String shortName;

  @Column(name = "long_name", nullable = false)
  private String longName;
}
