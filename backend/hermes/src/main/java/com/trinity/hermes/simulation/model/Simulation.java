package com.trinity.hermes.simulation.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
