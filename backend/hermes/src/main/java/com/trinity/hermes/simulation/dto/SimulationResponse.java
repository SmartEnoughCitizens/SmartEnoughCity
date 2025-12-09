package com.trinity.hermes.simulation.dto;

import com.trinity.hermes.simulation.model.SimulationResults;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {
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
