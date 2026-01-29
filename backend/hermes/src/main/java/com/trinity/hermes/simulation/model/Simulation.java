package com.trinity.hermes.simulation.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Simulation {
  private String id;
  private Long recommendationId;
  private String name;
  private String description;
  private String scenario;
  private String status;
  private String createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private SimulationResults results;


  public Simulation(
          String id,
          Long recommendationId,
          String name,
          String description,
          String scenario,
          String status,
          String createdBy,
          LocalDateTime createdAt,
          LocalDateTime completedAt,
          SimulationResults results) {

    this.id = id;
    this.recommendationId = recommendationId;
    this.name = name;
    this.description = description;
    this.scenario = scenario;
    this.status = status;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.completedAt = completedAt;
    this.results = results == null ? null : new SimulationResults(results);
  }

  public SimulationResults getResults() {
    return results == null ? null : new SimulationResults(results);
  }

  public void setResults(SimulationResults results) {
    this.results = results == null ? null : new SimulationResults(results);
  }

}
