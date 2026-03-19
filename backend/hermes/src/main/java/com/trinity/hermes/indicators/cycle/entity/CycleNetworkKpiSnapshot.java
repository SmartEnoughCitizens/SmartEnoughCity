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

/**
 * Hourly snapshot of the cycle network KPIs. Used as time-series input for the inference /
 * recommendation engine. One row per hour (deduplicated in service layer).
 */
@Entity
@Table(name = "cycle_network_kpi_snapshot", schema = "backend")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleNetworkKpiSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Timestamp truncated to the hour at which this snapshot was captured. */
  @Column(name = "snapshot_at", nullable = false, unique = true)
  private Instant snapshotAt;

  // ── NetworkSummaryDTO ──────────────────────────────────────────────────────

  @Column(name = "total_stations")
  private Integer totalStations;

  @Column(name = "total_bikes_available")
  private Integer totalBikesAvailable;

  @Column(name = "total_docks_available")
  private Integer totalDocksAvailable;

  @Column(name = "empty_stations")
  private Integer emptyStations;

  @Column(name = "full_stations")
  private Integer fullStations;

  @Column(name = "avg_network_fullness_pct")
  private Double avgNetworkFullnessPct;

  // ── NetworkKpiDTO ──────────────────────────────────────────────────────────

  @Column(name = "rebalancing_need_count")
  private Integer rebalancingNeedCount;

  @Column(name = "network_imbalance_score")
  private Double networkImbalanceScore;

  @Column(name = "avg_hourly_turnover_rate")
  private Double avgHourlyTurnoverRate;

  @Column(name = "daily_trips_estimate")
  private Long dailyTripsEstimate;

  @Column(name = "weekday_avg_usage_rate")
  private Double weekdayAvgUsageRate;

  @Column(name = "weekend_avg_usage_rate")
  private Double weekendAvgUsageRate;
}