package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "luas_stops", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramStop {

  @Column(name = "stop_id", nullable = false)
  private String stopId;

  @Column(name = "line")
  private String line;

  @Column(name = "name")
  private String name;

  @Column(name = "pronunciation")
  private String pronunciation;

  @Column(name = "park_ride")
  private Boolean parkRide;

  @Column(name = "cycle_ride")
  private Boolean cycleRide;

  @Column(name = "lat")
  private Double lat;

  @Column(name = "lon")
  private Double lon;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
