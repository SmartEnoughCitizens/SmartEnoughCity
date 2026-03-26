package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "tram_luas_stops", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramStop {

  @Id
  @Column(name = "stop_id", nullable = false)
  private String stopId;

  @Column(name = "line", nullable = false)
  private String line;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "pronunciation")
  private String pronunciation;

  @Column(name = "park_ride", nullable = false)
  private Boolean parkRide;

  @Column(name = "cycle_ride", nullable = false)
  private Boolean cycleRide;

  @Column(name = "lat", nullable = false)
  private Double lat;

  @Column(name = "lon", nullable = false)
  private Double lon;
}
