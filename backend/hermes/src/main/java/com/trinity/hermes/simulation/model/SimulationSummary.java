package com.trinity.hermes.simulation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSummary {
    private Double avgSpeed;
    private Double congestionLevel;
    private Double totalDelay;
    private Integer affectedVehicles;
}
