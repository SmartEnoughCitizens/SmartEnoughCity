package com.trinity.hermes.indicators.cycle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pre-computed daily network KPIs stored in the backend schema for fast dashboard reads. */
@Entity
@Table(name = "cycle_network_daily_kpi", schema = "backend")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleNetworkDailyKpi {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "metric_date", nullable = false, unique = true)
  private LocalDate metricDate;

  @Column(name = "total_stations")
  private Integer totalStations;

  @Column(name = "avg_available_bikes")
  private Double avgAvailableBikes;

  @Column(name = "avg_available_docks")
  private Double avgAvailableDocks;

  @Column(name = "peak_hour")
  private Integer peakHour;

  @Column(name = "total_trips_estimate")
  private Integer totalTripsEstimate;

  @Column(name = "avg_rebalancing_need")
  private Double avgRebalancingNeed;

  @Column(name = "avg_network_fullness_pct")
  private Double avgNetworkFullnessPct;

  @Column(name = "computed_at")
  private Instant computedAt;
}
