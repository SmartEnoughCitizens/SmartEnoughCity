package com.trinity.hermes.simulation; // Adjust package if needed

import java.util.List;

public class SimulationResults {
    private SimulationSummary summary;
    private List<String> recommendations;
    // Map visualizations if you want them later, ignoring for now

    public SimulationSummary getSummary() {
        return summary;
    }

    public void setSummary(SimulationSummary summary) {
        this.summary = summary;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}