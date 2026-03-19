package com.trinity.hermes.indicators.cycle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Weekly origin-destination flow snapshot. One row per (snapshot_date, origin, destination). Gives
 * the inference engine a demand matrix showing where bikes travel between stations.
 */
@Entity
@Table(
    name = "cycle_od_flow_snapshot",
    schema = "backend",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {"snapshot_date", "origin_station_id", "dest_station_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleOdFlowSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;

  @Column(name = "origin_station_id", nullable = false)
  private Integer originStationId;

  @Column(name = "origin_name")
  private String originName;

  @Column(name = "dest_station_id", nullable = false)
  private Integer destStationId;

  @Column(name = "dest_name")
  private String destName;

  /** Estimated number of trips from origin → destination in the last 30 days. */
  @Column(name = "estimated_trips")
  private Long estimatedTrips;
}
