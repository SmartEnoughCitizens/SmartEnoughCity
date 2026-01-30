package com.trinity.hermes.simulation.model;

import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SimulationResults {
  private SimulationSummary summary;
  private List<String> recommendations;

  public SimulationResults(SimulationResults other) {
    if (other == null) {
      return;
    }
    this.summary = other.summary == null ? null : new SimulationSummary(other.summary);
    this.recommendations =
        other.recommendations == null ? null : new ArrayList<>(other.recommendations);
  }

  public SimulationSummary getSummary() {
    return summary == null ? null : new SimulationSummary(summary);
  }

  public List<String> getRecommendations() {
    return recommendations == null ? null : new ArrayList<>(recommendations);
  }

  public void setSummary(SimulationSummary summary) {
    this.summary = summary == null ? null : new SimulationSummary(summary);
  }

  public void setRecommendations(List<String> recommendations) {
    this.recommendations = recommendations == null ? null : new ArrayList<>(recommendations);
  }
}
