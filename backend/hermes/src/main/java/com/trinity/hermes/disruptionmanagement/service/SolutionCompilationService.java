package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeRoute;
import com.trinity.hermes.disruptionmanagement.dto.DisruptionSolution;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for compiling and rating solution options for
 * disruptions.
 * Aggregates alternative routes and creates actionable recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionCompilationService {

    /**
     * Compile a comprehensive solution from alternative routes
     * 
     * @param disruption        The disruption to solve
     * @param alternativeRoutes List of calculated alternative routes
     * @return Compiled disruption solution
     */
    public DisruptionSolution compileSolution(Disruption disruption, List<AlternativeRoute> alternativeRoutes) {
        log.info("Compiling solution for disruption ID: {} with {} alternatives",
                disruption.getId(), alternativeRoutes.size());

        DisruptionSolution solution = new DisruptionSolution();

        // Set basic disruption information
        solution.setDisruptionId(disruption.getId());
        solution.setDisruptionType(disruption.getDisruptionType());
        solution.setSeverity(disruption.getSeverity());
        solution.setAffectedArea(disruption.getAffectedArea());
        solution.setAlternativeRoutes(alternativeRoutes);
        solution.setCalculatedAt(LocalDateTime.now());
        solution.setNumberOfOptionsEvaluated(alternativeRoutes.size());

        // Mark as ready for notification
        solution.setReadyForNotification(true);

        log.info("Solution compiled successfully for disruption {}", disruption.getId());
        return solution;
    }

    /**
     * Prioritize and rank alternative routes
     * 
     * @param routes List of routes to prioritize
     * @return Sorted list with highest priority first (by score descending)
     */
    public List<AlternativeRoute> prioritizeRoutes(List<AlternativeRoute> routes) {
        log.debug("Prioritizing {} routes", routes.size());

        if (routes == null || routes.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort by score (highest first)
        List<AlternativeRoute> prioritized = routes.stream()
                .sorted(Comparator.comparingInt(AlternativeRoute::getScore).reversed())
                .collect(Collectors.toList());

        // Log the prioritization
        for (int i = 0; i < prioritized.size(); i++) {
            AlternativeRoute route = prioritized.get(i);
            log.debug("Rank {}: {} (score: {})", i + 1, route.getRouteId(), route.getScore());
        }

        return prioritized;
    }

    /**
     * Select the primary recommendation from alternatives
     * 
     * @param alternatives List of alternative routes (should be prioritized)
     * @return The best route to recommend
     */
    public AlternativeRoute selectPrimaryRecommendation(List<AlternativeRoute> alternatives) {
        log.debug("Selecting primary recommendation from {} alternatives", alternatives.size());

        if (alternatives == null || alternatives.isEmpty()) {
            log.warn("No alternatives available for primary recommendation");
            return null;
        }

        // Select the first route (highest score if list is prioritized)
        AlternativeRoute primary = alternatives.get(0);
        log.info("Selected primary recommendation: {} (score: {})",
                primary.getRouteId(), primary.getScore());

        return primary;
    }

    /**
     * Select secondary recommendations
     * 
     * @param alternatives          List of alternative routes (should be
     *                              prioritized)
     * @param primaryRecommendation The primary recommendation
     * @param maxSecondary          Maximum number of secondary recommendations
     * @return List of secondary recommendations
     */
    public List<AlternativeRoute> selectSecondaryRecommendations(
            List<AlternativeRoute> alternatives,
            AlternativeRoute primaryRecommendation,
            int maxSecondary) {

        log.debug("Selecting up to {} secondary recommendations", maxSecondary);

        if (alternatives == null || alternatives.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter out primary and take next best options
        List<AlternativeRoute> secondary = alternatives.stream()
                .filter(route -> primaryRecommendation == null ||
                        !route.getRouteId().equals(primaryRecommendation.getRouteId()))
                .limit(maxSecondary)
                .collect(Collectors.toList());

        log.info("Selected {} secondary recommendations", secondary.size());
        return secondary;
    }

    /**
     * Generate user-friendly action summary
     * 
     * @param solution The compiled solution
     * @return Summary text for users
     */
    public String generateActionSummary(DisruptionSolution solution) {
        log.debug("Generating action summary for disruption {}", solution.getDisruptionId());

        if (solution.getPrimaryRecommendation() == null) {
            return String.format("Disruption detected: %s in %s. No alternative routes available at this time.",
                    solution.getDisruptionType(), solution.getAffectedArea());
        }

        AlternativeRoute primary = solution.getPrimaryRecommendation();

        // Format: "Take [transport mode] instead of [affected area]. Estimated time:
        // [time] minutes."
        String summary = String.format(
                "Take %s instead. Route: %s. Estimated time: %d minutes%s",
                primary.getTransportMode(),
                primary.getRouteName(),
                primary.getEstimatedTimeMinutes(),
                primary.getNumberOfTransfers() > 0
                        ? String.format(" (%d transfer%s)",
                                primary.getNumberOfTransfers(),
                                primary.getNumberOfTransfers() > 1 ? "s" : "")
                        : "");

        log.debug("Generated summary: {}", summary);
        return summary;
    }

    /**
     * Generate step-by-step instructions for the recommended route
     * 
     * @param route The recommended route
     * @return List of step-by-step instructions
     */
    public List<String> generateInstructions(AlternativeRoute route) {
        log.debug("Generating instructions for route {}", route.getRouteId());

        List<String> instructions = new ArrayList<>();

        if (route == null) {
            return instructions;
        }

        // Parse the route name to generate steps
        String transportMode = route.getTransportMode();
        String routeName = route.getRouteName();
        int estimatedTime = route.getEstimatedTimeMinutes();
        int walkingMeters = route.getWalkingDistanceMeters();

        // Generate instructions based on transport mode
        if (transportMode.contains("+") || transportMode.contains("→")) {
            // Multi-modal route
            generateMultiModalInstructions(route, instructions);
        } else {
            // Single mode route
            generateSingleModeInstructions(route, instructions);
        }

        // Add walking instruction if significant walking distance
        if (walkingMeters > 100) {
            int walkingMinutes = walkingMeters / 80; // Approximate 80m/min walking speed
            instructions.add(String.format("Walk approximately %d meters to the stop (%d minutes)",
                    walkingMeters, walkingMinutes));
        }

        // Add final instruction
        instructions.add(String.format("Total estimated journey time: %d minutes", estimatedTime));

        log.debug("Generated {} instruction steps", instructions.size());
        return instructions;
    }

    /**
     * Estimate the overall impact of the disruption on users following the solution
     * 
     * @param disruption The disruption
     * @param solution   The compiled solution
     * @return Impact description
     */
    public String estimateImpact(Disruption disruption, DisruptionSolution solution) {
        log.debug("Estimating impact for disruption {}", disruption.getId());

        // Base estimate on severity and number of affected routes
        int routeCount = disruption.getAffectedRoutes() != null
                ? disruption.getAffectedRoutes().size()
                : 1;

        String severity = disruption.getSeverity();
        int baseImpact = 200; // Base passengers per route

        // Calculate estimated affected travelers
        int estimatedTravelers = baseImpact * Math.max(1, routeCount);

        // Adjust by severity
        switch (severity.toUpperCase()) {
            case "CRITICAL" -> estimatedTravelers = (int) (estimatedTravelers * 2.5);
            case "HIGH" -> estimatedTravelers = (int) (estimatedTravelers * 1.8);
            case "MEDIUM" -> estimatedTravelers = (int) (estimatedTravelers * 1.2);
            default -> estimatedTravelers = estimatedTravelers;
        }

        // Calculate additional delay
        Integer delayMinutes = disruption.getDelayMinutes();
        AlternativeRoute primary = solution.getPrimaryRecommendation();
        int additionalDelay = 0;

        if (delayMinutes != null && primary != null) {
            // Assume normal journey is disruption delay minus some buffer
            int normalTime = Math.max(10, delayMinutes - 5);
            additionalDelay = primary.getEstimatedTimeMinutes() - normalTime;
        }

        // Generate impact string
        String impact = String.format(
                "Estimated %d-%d travelers affected. ",
                estimatedTravelers - 100,
                estimatedTravelers + 100);

        if (additionalDelay > 0) {
            impact += String.format("Additional %d minutes expected on alternative route.", additionalDelay);
        } else {
            impact += "Alternative route offers similar journey time.";
        }

        log.info("Impact estimate: {}", impact);
        return impact;
    }

    /**
     * Determine which user groups should be notified
     * 
     * @param disruption The disruption
     * @return List of affected user groups
     */
    public List<String> identifyAffectedUserGroups(Disruption disruption) {
        log.debug("Identifying affected user groups for disruption {}", disruption.getId());

        List<String> affectedGroups = new ArrayList<>();

        // Identify groups based on routes and area
        List<String> routes = disruption.getAffectedRoutes();
        String area = disruption.getAffectedArea();

        // Add commuters for any disruption
        if (routes != null && !routes.isEmpty()) {
            affectedGroups.add("Commuters on routes: " + String.join(", ", routes));
        }

        // Add area-specific groups
        if (area != null) {
            if (area.toLowerCase().contains("center") || area.toLowerCase().contains("central")) {
                affectedGroups.add("City Center Travelers");
                affectedGroups.add("Office Workers");
                affectedGroups.add("Shoppers");
            }

            if (area.toLowerCase().contains("airport")) {
                affectedGroups.add("Air Travelers");
                affectedGroups.add("Airport Staff");
            }

            if (area.toLowerCase().contains("station")) {
                affectedGroups.add("Rail Commuters");
                affectedGroups.add("Through Passengers");
            }
        }

        // Add time-based groups (would use actual time in production)
        affectedGroups.add("General Public");

        log.info("Identified {} affected user groups", affectedGroups.size());
        return affectedGroups;
    }

    // ===== PRIVATE HELPER METHODS =====

    private void generateMultiModalInstructions(AlternativeRoute route, List<String> instructions) {
        String routeName = route.getRouteName();

        // Parse the route name (e.g., "Metro Line 2 → Bus 27" or "Bus + Tram")
        String[] parts = routeName.split("→|\\+");

        int stepNumber = 1;
        for (String part : parts) {
            part = part.trim();
            instructions.add(String.format("Step %d: Take %s", stepNumber++, part));
        }

        if (route.getNumberOfTransfers() > 0) {
            instructions.add(String.format("Note: This route requires %d transfer%s",
                    route.getNumberOfTransfers(),
                    route.getNumberOfTransfers() > 1 ? "s" : ""));
        }
    }

    private void generateSingleModeInstructions(AlternativeRoute route, List<String> instructions) {
        String transportMode = route.getTransportMode();
        String routeName = route.getRouteName();

        instructions.add(String.format("Step 1: Board %s service: %s", transportMode, routeName));

        if (route.getDescription() != null && !route.getDescription().isEmpty()) {
            instructions.add(String.format("Details: %s", route.getDescription()));
        }

        instructions.add("Step 2: Travel to your destination");
    }
}
