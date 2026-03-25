package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "tram_trips", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramTrip {

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "route_id", nullable = false)
  private String routeId;

  @Column(name = "service_id", nullable = false)
  private Integer serviceId;

  @Column(nullable = false)
  private String headsign;

  @Column(name = "short_name", nullable = false)
  private String shortName;

  @Column(name = "direction_id", nullable = false)
  private Integer directionId;

  @Column(name = "shape_id", nullable = false)
  private String shapeId;
}
