package com.trinity.hermes.indicators.cycle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "dublin_bikes_station_snapshots", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DublinBikesSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "station_id", nullable = false)
  private Integer stationId;

  @Column(nullable = false)
  private Instant timestamp;

  @Column(name = "last_reported", nullable = false)
  private Instant lastReported;

  @Column(name = "available_bikes", nullable = false)
  private Integer availableBikes;

  @Column(name = "available_docks", nullable = false)
  private Integer availableDocks;

  @Column(name = "disabled_bikes")
  private Integer disabledBikes;

  @Column(name = "disabled_docks")
  private Integer disabledDocks;

  @Column(name = "is_installed", nullable = false)
  private Boolean isInstalled;

  @Column(name = "is_renting", nullable = false)
  private Boolean isRenting;

  @Column(name = "is_returning", nullable = false)
  private Boolean isReturning;

  @Column(name = "created_at")
  private Instant createdAt;
}
