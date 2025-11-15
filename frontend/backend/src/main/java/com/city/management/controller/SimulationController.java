package com.city.management.controller;

import com.city.management.model.Simulation;
import com.city.management.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    @Autowired
    private MockDataService mockDataService;

    @GetMapping
    public ResponseEntity<List<Simulation>> getAllSimulations() {
        return ResponseEntity.ok(mockDataService.getAllSimulations());
    }

    @PostMapping("/run")
    public ResponseEntity<Simulation> runSimulation(
            @RequestBody Map<String, Object> parameters,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        Simulation simulation = mockDataService.runSimulation(parameters, username);
        return ResponseEntity.ok(simulation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSimulation(@PathVariable Long id) {
        boolean deleted = mockDataService.deleteSimulation(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Simulation deleted successfully"));
    }
}
