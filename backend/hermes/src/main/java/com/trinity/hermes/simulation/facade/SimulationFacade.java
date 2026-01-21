package com.trinity.hermes.simulation.facade;

import com.trinity.hermes.simulation.dto.RunSimulationRequest;
import com.trinity.hermes.simulation.dto.SimulationResponse;
import com.trinity.hermes.simulation.model.Simulation;
import com.trinity.hermes.simulation.service.SimulationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade for simulation operations. Handles orchestration between controller and service layers.
 */
@Component
@RequiredArgsConstructor
public class SimulationFacade {

  private final SimulationService simulationService;

  /**
   * Run a simulation based on a recommendation.
   *
   * @param request the run simulation request containing recommendationId
   * @return the simulation response, or empty if recommendation not found
   */
  public Optional<SimulationResponse> runSimulation(RunSimulationRequest request) {
    return simulationService.runSimulation(request.getRecommendationId()).map(this::mapToResponse);
  }

  /** Map internal Simulation model to SimulationResponse DTO. */
  private SimulationResponse mapToResponse(Simulation simulation) {
    return new SimulationResponse(
        simulation.getId(),
        simulation.getRecommendationId(),
        simulation.getName(),
        simulation.getDescription(),
        simulation.getScenario(),
        simulation.getStatus(),
        simulation.getCreatedBy(),
        simulation.getCreatedAt(),
        simulation.getCompletedAt(),
        simulation.getResults());
  }
}
