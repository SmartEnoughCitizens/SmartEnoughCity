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
 * Per-station daily metrics snapshot. One row per (station_id, metric_date). Derived from station
 * rankings and live snapshot data. Used by the inference engine to learn per-station behaviour
 * patterns over time.
 */
@Entity
@Table(
    name = "cycle_station_daily_metrics",
    schema = "backend",
    uniqueConstraints = @UniqueConstraint(columnNames = {"station_id", "metric_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleStationDailyMetrics {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "station_id", nullable = false)
  private Integer stationId;

  @Column(name = "station_name")
  private String stationName;

  @Column(name = "metric_date", nullable = false)
  private LocalDate metricDate;

  /** Average bike usage rate over the day (0–100). Derived from busiest/underused ranking query. */
  @Column(name = "avg_usage_rate_pct")
  private Double avgUsageRatePct;

  /** Rank in the "busiest stations" list for this day. Null if outside top-N. */
  @Column(name = "busiest_rank")
  private Integer busiestRank;

  /** Rank in the "underused stations" list for this day. Null if outside top-N. */
  @Column(name = "underused_rank")
  private Integer underusedRank;

  /** Bike availability percentage from the live snapshot at capture time. */
  @Column(name = "bike_availability_pct")
  private Double bikeAvailabilityPct;

  /** Dock availability percentage from the live snapshot at capture time. */
  @Column(name = "dock_availability_pct")
  private Double dockAvailabilityPct;

  /** Live status colour (RED / YELLOW / GREEN) at capture time. */
  @Column(name = "status_color", length = 10)
  private String statusColor;
}