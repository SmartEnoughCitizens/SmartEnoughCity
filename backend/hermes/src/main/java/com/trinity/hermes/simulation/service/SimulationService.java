package com.trinity.hermes.simulation.service;

import com.trinity.hermes.recommendation.dto.RecommendationResponse;
import com.trinity.hermes.recommendation.service.RecommendationService;
import com.trinity.hermes.simulation.model.Simulation;
import com.trinity.hermes.simulation.model.SimulationResults;
import com.trinity.hermes.simulation.model.SimulationSummary;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for running simulations based on recommendations. Generates dummy simulation data for the
 * thin slice demo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

  private final RecommendationService recommendationService;

  /**
   * Run a simulation based on a recommendation ID. Fetches the recommendation details and generates
   * a simulation showing what would happen if the recommendation was implemented.
   *
   * @param recommendationId the ID of the recommendation to simulate
   * @return the simulation results, or empty if recommendation not found
   */
  public Optional<Simulation> runSimulation(Long recommendationId) {
    log.info("Running simulation for recommendation ID: {}", recommendationId);

    // Fetch the recommendation
    Optional<RecommendationResponse> recommendationOpt =
        recommendationService.getRecommendationById(recommendationId);

    if (recommendationOpt.isEmpty()) {
      log.warn("Recommendation not found with ID: {}", recommendationId);
      return Optional.empty();
    }

    RecommendationResponse recommendation = recommendationOpt.get();

    // Generate dummy simulation data based on the recommendation
    // This logic is based on the commented-out Python code
    SimulationResults results = generateDummyResults(recommendation);

    // Build the simulation object
    Simulation simulation = new Simulation();
    simulation.setId(UUID.randomUUID().toString());
    simulation.setRecommendationId(recommendationId);
    simulation.setName("Simulation for: " + recommendation.getId());
    simulation.setDescription(
        "Impact analysis if recommendation '"
            + recommendation.getDataIndicator()
            + "' is implemented");
    simulation.setScenario("recommendation-impact");
    simulation.setStatus("completed");
    simulation.setCreatedBy("demo-user");
    simulation.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")));
    simulation.setCompletedAt(LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")));
    simulation.setResults(results);

    log.info("Simulation completed successfully: {}", simulation.getId());
    return Optional.of(simulation);
  }

  /**
   * Generate dummy simulation results based on recommendation. Logic adapted from the commented-out
   * Python code.
   */
  private SimulationResults generateDummyResults(RecommendationResponse recommendation) {
    // Base values for demo
    double baseCongestion = 40.0;
    double factor = 1.0;

    // Apply a modification factor based on recommendation status
    // (simulating that implementing the recommendation reduces congestion)
    if ("APPROVED".equalsIgnoreCase(recommendation.getStatus())) {
      factor = 0.7; // Approved recommendations reduce congestion
    } else if ("PENDING".equalsIgnoreCase(recommendation.getStatus())) {
      factor = 0.85; // Pending recommendations have moderate impact
    } else {
      factor = 1.0; // Default factor
    }

    // Calculate simulation metrics
    double calculatedCongestion = Math.min(100.0, baseCongestion * factor);
    double avgSpeed = Math.max(5.0, 50.0 / factor);
    double totalDelay = Math.round(10 * factor * 10.0) / 10.0;
    int affectedVehicles = (int) (100 * factor);

    // Build summary
    SimulationSummary summary = new SimulationSummary();
    summary.setAvgSpeed(Math.round(avgSpeed * 10.0) / 10.0);
    summary.setCongestionLevel(Math.round(calculatedCongestion * 10.0) / 10.0);
    summary.setTotalDelay(totalDelay);
    summary.setAffectedVehicles(affectedVehicles);

    // Build results with recommendations
    SimulationResults results = new SimulationResults();
    results.setSummary(summary);
    results.setRecommendations(
        Arrays.asList(
            "Optimize traffic lights based on recommendation",
            "Deploy additional units to affected areas",
            "Monitor real-time traffic flow after implementation"));

    return results;
  }
}
