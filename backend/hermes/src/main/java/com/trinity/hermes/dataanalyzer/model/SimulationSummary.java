package com.trinity.hermes.dataanalyzer.model; // Adjust package if needed

public class SimulationSummary {
    private Double avgSpeed;
    private Double congestionLevel;
    private Double totalDelay;
    private Integer affectedVehicles;

    // Getters and Setters
    public Double getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(Double avgSpeed) { this.avgSpeed = avgSpeed; }

    public Double getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; }

    public Double getTotalDelay() { return totalDelay; }
    public void setTotalDelay(Double totalDelay) { this.totalDelay = totalDelay; }

    public Integer getAffectedVehicles() { return affectedVehicles; }
    public void setAffectedVehicles(Integer affectedVehicles) { this.affectedVehicles = affectedVehicles; }
}