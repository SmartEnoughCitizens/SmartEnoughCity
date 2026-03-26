package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "tram_luas_forecasts", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramLuasForecast {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "stop_id", nullable = false)
  private String stopId;

  @Column(name = "line", nullable = false)
  private String line;

  @Column(name = "direction", nullable = false)
  private String direction;

  @Column(name = "destination", nullable = false)
  private String destination;

  @Column(name = "due_mins")
  private Integer dueMins;

  @Column(name = "message", nullable = false)
  private String message;
}
