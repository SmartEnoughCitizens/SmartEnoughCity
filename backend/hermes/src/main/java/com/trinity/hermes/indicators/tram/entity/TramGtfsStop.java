package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** GTFS stop from tram_stops table (distinct from tram_luas_stops used by forecast API). */
@Entity
@Table(name = "tram_stops", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramGtfsStop {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @Column(name = "code")
  private Integer code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "lat", nullable = false)
  private Double lat;

  @Column(name = "lon", nullable = false)
  private Double lon;
}
