package com.trinity.hermes.simulation;

import java.util.List;
import java.util.Map;

public class SimulationParameters {
    private Integer duration;
    private Double trafficIncrease;
    private String transportMode; // Added for Demo
    private Double modificationFactor; // Added for Demo
    private List<String> affectedRoutes;
    private String weatherConditions;
    private Map<String, Object> customParams;

    // Getters and Setters
    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public Double getModificationFactor() {
        return modificationFactor;
    }

    public void setModificationFactor(Double modificationFactor) {
        this.modificationFactor = modificationFactor;
    }

    public Double getTrafficIncrease() {
        return trafficIncrease;
    }
}