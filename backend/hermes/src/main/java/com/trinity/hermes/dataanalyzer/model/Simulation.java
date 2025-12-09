package com.trinity.hermes.dataanalyzer.model; // Adjust package if needed

public class Simulation {
    private String id;
    private String name;
    private String description;
    private String scenario;
    private SimulationParameters parameters;
    private String status;
    private String createdBy;
    private String createdAt;
    private String completedAt;
    private SimulationResults results;

    // Getters and Setters (Omitted for brevity, generate these in IntelliJ: Alt+Insert -> Getter and Setter -> Select All)
    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public void setParameters(SimulationParameters parameters) { this.parameters = parameters; }
    public SimulationParameters getParameters() { return parameters; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public void setResults(SimulationResults results) { this.results = results; }
    public SimulationResults getResults() { return results; }
}