package com.trinity.hermes.service;

import com.trinity.hermes.dto.CreateSimulationRequest;
import com.trinity.hermes.model.Simulation;
import com.trinity.hermes.model.SimulationResults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SimulationService {

    @Value("${inference.engine.url:http://localhost:8000}")
    private String inferenceEngineUrl;

    private final RestTemplate restTemplate;

    public SimulationService() {
        this.restTemplate = new RestTemplate();
    }

    public Simulation runSimulation(CreateSimulationRequest request) {
        // 1. Prepare the endpoint
        String pythonEndpoint = inferenceEngineUrl + "/predict-impact";

        // 2. Call Python Engine (sending the parameters part of the request)
        // We assume Python returns a SimulationResults object
        SimulationResults results = restTemplate.postForObject(
                pythonEndpoint,
                request.getParameters(), 
                SimulationResults.class
        );

        // 3. Construct the full Simulation object to return to Frontend
        Simulation simulation = new Simulation();
        simulation.setId(UUID.randomUUID().toString());
        simulation.setName(request.getName());
        simulation.setDescription(request.getDescription());
        simulation.setScenario(request.getScenario());
        simulation.setParameters(request.getParameters());
        simulation.setStatus("completed");
        simulation.setCreatedAt(LocalDateTime.now().toString());
        simulation.setCompletedAt(LocalDateTime.now().toString());
        simulation.setCreatedBy("demo-user"); // Hardcoded for thin slice
        simulation.setResults(results);

        return simulation;
    }
}