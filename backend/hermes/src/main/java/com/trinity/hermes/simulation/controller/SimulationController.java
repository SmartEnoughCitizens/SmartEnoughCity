package com.trinity.hermes.simulation.controller;

import com.trinity.hermes.simulation.dto.RunSimulationRequest;
import com.trinity.hermes.simulation.dto.SimulationResponse;
import com.trinity.hermes.simulation.facade.SimulationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST Controller for Simulation operations.
 * Provides endpoints for running impact simulations based on recommendations.
 */
@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final SimulationFacade simulationFacade;

    /**
     * Run a simulation based on a recommendation.
     * 
     * POST /api/v1/simulations/run
     * 
     * @param request the request containing the recommendationId to simulate
     * @return the simulation results, or 404 if recommendation not found
     */
    @PostMapping("/run")
    public ResponseEntity<SimulationResponse> runSimulation(@RequestBody RunSimulationRequest request) {
        log.info("Received simulation request for recommendation ID: {}", request.getRecommendationId());

        Optional<SimulationResponse> simulation = simulationFacade.runSimulation(request);

        return simulation
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}