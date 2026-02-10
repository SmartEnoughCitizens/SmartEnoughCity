package com.trinity.hermes.simulation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SimulationSummary {
  private Double avgSpeed;
  private Double congestionLevel;
  private Double totalDelay;
  private Integer affectedVehicles;

  public SimulationSummary(
      Double avgSpeed, Double congestionLevel, Double totalDelay, Integer affectedVehicles) {
    this.avgSpeed = avgSpeed;
    this.congestionLevel = congestionLevel;
    this.totalDelay = totalDelay;
    this.affectedVehicles = affectedVehicles;
  }

  public SimulationSummary(SimulationSummary other) {
    if (other == null) {
      return;
    }
    this.avgSpeed = other.avgSpeed;
    this.congestionLevel = other.congestionLevel;
    this.totalDelay = other.totalDelay;
    this.affectedVehicles = other.affectedVehicles;
  }
}
