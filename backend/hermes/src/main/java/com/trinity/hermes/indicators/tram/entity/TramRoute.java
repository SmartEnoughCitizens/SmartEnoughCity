package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "tram_routes", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramRoute {

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "agency_id", nullable = false)
  private Integer agencyId;

  @Column(name = "short_name", nullable = false)
  private String shortName;

  @Column(name = "long_name", nullable = false)
  private String longName;

  @Column(name = "route_color")
  private String routeColor;

  @Column(name = "route_text_color")
  private String routeTextColor;
}
