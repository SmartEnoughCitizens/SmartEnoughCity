package com.trinity.hermes.simulation.controller;

import com.trinity.hermes.simulation.SimulationService;
import com.trinity.hermes.simulation.dto.CreateSimulationRequest;
import com.trinity.hermes.simulation.model.Simulation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "http://localhost:3000") // Allow React Frontend
public class SimulationController {

    private final SimulationService simulationService;

    @Autowired
    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/run")
    public ResponseEntity<Simulation> runSimulation(@RequestBody CreateSimulationRequest request) {
        Simulation simulation = simulationService.runSimulation(request);
        return ResponseEntity.ok(simulation);
    }
}