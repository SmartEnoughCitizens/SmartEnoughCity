package com.trinity.hermes.simulation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResults {
    private SimulationSummary summary;
    private List<String> recommendations;
}
