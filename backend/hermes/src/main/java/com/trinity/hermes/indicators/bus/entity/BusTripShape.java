package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_trip_shapes", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusTripShape {

  @Id
  @Column(name = "entry_id")
  private Integer entryId;

  @Column(name = "shape_id", nullable = false)
  private String shapeId;

  @Column(name = "pt_sequence", nullable = false)
  private Integer ptSequence;

  @Column(name = "pt_lat", nullable = false)
  private Double ptLat;

  @Column(name = "pt_lon", nullable = false)
  private Double ptLon;

  @Column(name = "dist_traveled", nullable = false)
  private Double distTraveled;
}
