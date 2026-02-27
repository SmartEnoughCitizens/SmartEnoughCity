package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bus_route_metrics", schema = "backend")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteMetrics {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "route_id", nullable = false, unique = true)
  private String routeId;

  @Column(name = "route_short_name")
  private String routeShortName;

  @Column(name = "route_long_name")
  private String routeLongName;

  @Column(name = "active_vehicles")
  private Integer activeVehicles;

  @Column(name = "scheduled_trips")
  private Integer scheduledTrips;

  @Column(name = "utilization_pct")
  private Double utilizationPct;

  @Column(name = "avg_delay_seconds")
  private Double avgDelaySeconds;

  @Column(name = "max_delay_seconds")
  private Integer maxDelaySeconds;

  @Column(name = "avg_occupancy_pct")
  private Double avgOccupancyPct;

  @Column(name = "peak_occupancy_pct")
  private Double peakOccupancyPct;

  @Column(name = "reliability_pct")
  private Double reliabilityPct;

  @Column(name = "late_arrival_pct")
  private Double lateArrivalPct;

  private String status;

  @Column(name = "computed_at")
  private Timestamp computedAt;
}
