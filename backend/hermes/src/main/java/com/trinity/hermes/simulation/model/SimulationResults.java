package com.trinity.hermes.simulation.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResults {
  private SimulationSummary summary;
  private List<String> recommendations;
}
