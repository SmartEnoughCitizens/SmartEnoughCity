package com.trinity.hermes.dto;

import com.trinity.hermes.model.SimulationParameters;

public class CreateSimulationRequest {
    private String name;
    private String description;
    private String scenario;
    private SimulationParameters parameters;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public SimulationParameters getParameters() { return parameters; }
    public void setParameters(SimulationParameters parameters) { this.parameters = parameters; }
}