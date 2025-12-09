package com.trinity.hermes.simulation;

import com.trinity.hermes.simulation.dto.CreateSimulationRequest;
import com.trinity.hermes.simulation.model.Simulation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SimulationService {

    private final RestTemplate restTemplate;

    public SimulationService() {
        this.restTemplate = new RestTemplate();
    }

    public Simulation runSimulation(CreateSimulationRequest request) {
        // 1. Prepare the endpoint - Skipped, will load from database instead
        // String pythonEndpoint = inferenceEngineUrl + "/predict-impact";

        // 2. Load SimulationResults object data

        // logger.info(f"Received simulation request: {params}")

        // # 1. Basic Logic for Demo
        // base_congestion = 40.0
        // factor = params.modificationFactor if params.modificationFactor is not None
        // else 1.0

        // # Logic: If traffic increases, congestion goes up
        // if params.trafficIncrease:
        // factor += (params.trafficIncrease / 100.0)

        // # Logic: Weather impact
        // if params.weatherConditions == 'rain':
        // factor *= 1.2

        // calculated_congestion = min(100.0, base_congestion * factor)
        // avg_speed = max(5.0, 50.0 / factor) # Speed drops as factor increases

        // # 2. Return result
        // return {
        // "summary": {
        // "avgSpeed": round(avg_speed, 1),
        // "congestionLevel": round(calculated_congestion, 1),
        // "totalDelay": round(10 * factor, 1),
        // "affectedVehicles": int(100 * factor)
        // },
        // "recommendations": [
        // f"Optimize traffic lights for {params.transportMode}",
        // "Deploy 2 additional units to Sector 4"
        // ]
        // }

        SimulationResults results = restTemplate.postForObject(
                pythonEndpoint,
                request.getParameters(),
                SimulationResults.class);

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