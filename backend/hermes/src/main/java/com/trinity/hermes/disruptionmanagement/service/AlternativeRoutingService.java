package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.indicators.cycle.entity.CycleStation;
import com.trinity.hermes.indicators.cycle.repository.CycleStationRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import com.trinity.hermes.indicators.tram.entity.LuasStop;
import com.trinity.hermes.indicators.tram.repository.LuasStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service responsible for calculating alternative routes when disruptions
 * occur.
 * This integrates with routing algorithms and transport data to find viable
 * alternatives.
 * 
 * NOTE: This is a MOCK implementation for the thin slice.
 * In production, this would integrate with real routing APIs (GraphHopper,
 * OSRM, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlternativeRoutingService {

    private final LuasStopRepository luasStopRepository;
    private final CycleStationRepository cycleStationRepository;
    private final TrainStationRepository trainStationRepository;

    private final Random random = new Random();

    /**
     * Calculate alternative routes based on the disruption details
     * 
     * @param disruption The detected disruption
     * @return List of alternative routes (2-4 mock alternatives)
     */
    public List<AlternativeRoute> calculateAlternativeRoutes(Disruption disruption) {
        log.info("Calculating alternative routes for disruption ID: {}", disruption.getId());

        List<AlternativeRoute> alternatives = new ArrayList<>();

        // Get the primary affected transport mode
        String primaryMode = getPrimaryAffectedMode(disruption);
        log.info("Primary affected transport mode: {}", primaryMode);

        // Generate alternative routes based on the affected mode
        switch (primaryMode.toUpperCase()) {
            case "BUS" -> alternatives.addAll(generateBusAlternatives(disruption));
            case "TRAM" -> alternatives.addAll(generateTramAlternatives(disruption));
            case "METRO" -> alternatives.addAll(generateMetroAlternatives(disruption));
            case "TRAIN" -> alternatives.addAll(generateTrainAlternatives(disruption));
            default -> alternatives.addAll(generateGenericAlternatives(disruption));
        }

        log.info("Generated {} alternative routes", alternatives.size());
        return alternatives;
    }

    /**
     * Calculate alternative routes for specific origin and destination
     * 
     * @param disruption The disruption affecting the route
     * @param originLat  Origin latitude
     * @param originLon  Origin longitude
     * @param destLat    Destination latitude
     * @param destLon    Destination longitude
     * @return List of alternative routes
     */
    public List<AlternativeRoute> calculateRoutesForPoints(
            Disruption disruption,
            Double originLat,
            Double originLon,
            Double destLat,
            Double destLon) {

        log.info("Calculating routes from ({}, {}) to ({}, {}) avoiding disruption {}",
                originLat, originLon, destLat, destLon, disruption.getId());

        // For thin slice, use the same logic as general alternatives
        return calculateAlternativeRoutes(disruption);
    }

    /**
     * Evaluate and score a route based on various criteria
     * 
     * @param route      The route to evaluate
     * @param disruption Context of the disruption
     * @return Scored route with updated metrics
     */
    public AlternativeRoute evaluateRoute(AlternativeRoute route, Disruption disruption) {
        log.debug("Evaluating route: {}", route.getRouteId());

        int score = 0;

        // Factor 1: Time efficiency (30 points max)
        // Lower time = higher score
        int estimatedTime = route.getEstimatedTimeMinutes();
        if (estimatedTime <= 20) {
            score += 30;
        } else if (estimatedTime <= 35) {
            score += 20;
        } else if (estimatedTime <= 50) {
            score += 10;
        }

        // Factor 2: Transfer count (20 points max)
        // Fewer transfers = higher score
        int transfers = route.getNumberOfTransfers();
        if (transfers == 0) {
            score += 20;
        } else if (transfers == 1) {
            score += 15;
        } else if (transfers == 2) {
            score += 10;
        }

        // Factor 3: Walking distance (20 points max)
        // Less walking = higher score
        int walkingMeters = route.getWalkingDistanceMeters();
        if (walkingMeters <= 200) {
            score += 20;
        } else if (walkingMeters <= 500) {
            score += 15;
        } else if (walkingMeters <= 1000) {
            score += 10;
        }

        // Factor 4: Reliability (30 points max)
        // Metro > Tram > Bus (in general)
        String transportMode = route.getTransportMode().toUpperCase();
        if (transportMode.contains("METRO")) {
            score += 30;
        } else if (transportMode.contains("TRAM")) {
            score += 25;
        } else if (transportMode.contains("BUS")) {
            score += 20;
        } else if (transportMode.contains("WALK")) {
            score += 15; // Walking is reliable but slow
        }

        route.setScore(score);
        log.debug("Route {} scored: {}/100", route.getRouteId(), score);

        return route;
    }

    /**
     * Find the best alternative route from a list
     * 
     * @param alternatives List of alternative routes
     * @return The best alternative route
     */
    public AlternativeRoute findBestAlternative(List<AlternativeRoute> alternatives) {
        log.debug("Finding best alternative from {} options", alternatives.size());

        if (alternatives.isEmpty()) {
            return null;
        }

        // Sort by score (highest first)
        return alternatives.stream()
                .max((r1, r2) -> Integer.compare(r1.getScore(), r2.getScore()))
                .orElse(alternatives.get(0));
    }

    // ===== PRIVATE HELPER METHODS FOR MOCK ROUTE GENERATION =====

    private String getPrimaryAffectedMode(Disruption disruption) {
        List<String> modes = disruption.getAffectedTransportModes();
        if (modes != null && !modes.isEmpty()) {
            return modes.get(0);
        }
        return "BUS"; // Default
    }

    private List<AlternativeRoute> generateBusAlternatives(Disruption disruption) {
        List<AlternativeRoute> alternatives = new ArrayList<>();
        int baseDelay = disruption.getDelayMinutes() != null ? disruption.getDelayMinutes() : 20;

        // Alternative 1: Metro + Bus
        AlternativeRoute alt1 = new AlternativeRoute();
        alt1.setRouteId("ALT_METRO_BUS_1");
        alt1.setTransportMode("Metro + Bus");
        alt1.setRouteName("Metro Line 2 → Bus 27");
        alt1.setEstimatedTimeMinutes(baseDelay + 10);
        alt1.setNumberOfTransfers(1);
        alt1.setWalkingDistanceMeters(300);
        alt1.setDescription("Take Metro Line 2 to Central, transfer to Bus 27");
        alternatives.add(alt1);

        // Alternative 2: Tram (Dynamic from DB)
        AlternativeRoute alt2 = new AlternativeRoute();
        alt2.setRouteId("ALT_TRAM_1");
        alt2.setTransportMode("Tram");

        // Fetch a real Luas line/stop if available
        List<LuasStop> luasStops = luasStopRepository.findAll();
        String stopName = luasStops.isEmpty() ? "Lines" : luasStops.get(0).getName();
        String lineName = luasStops.isEmpty() ? "Luas Green Line" : luasStops.get(0).getLine();

        alt2.setRouteName("Tram from " + stopName);
        alt2.setEstimatedTimeMinutes(baseDelay + 15);
        alt2.setNumberOfTransfers(0);
        alt2.setWalkingDistanceMeters(200);
        alt2.setDescription("Direct tram service on " + lineName);
        alternatives.add(alt2);

        // Alternative 3: Alternative Bus Route
        AlternativeRoute alt3 = new AlternativeRoute();
        alt3.setRouteId("ALT_BUS_2");
        alt3.setTransportMode("Bus");
        alt3.setRouteName("Bus 42 → Bus 18");
        alt3.setEstimatedTimeMinutes(baseDelay + 20);
        alt3.setNumberOfTransfers(1);
        alt3.setWalkingDistanceMeters(400);
        alt3.setDescription("Alternative bus route via Bus 42 and 18");
        alternatives.add(alt3);

        return alternatives;
    }

    private List<AlternativeRoute> generateTramAlternatives(Disruption disruption) {
        List<AlternativeRoute> alternatives = new ArrayList<>();
        int baseDelay = disruption.getDelayMinutes() != null ? disruption.getDelayMinutes() : 20;

        // Alternative 1: Bus
        AlternativeRoute alt1 = new AlternativeRoute();
        alt1.setRouteId("ALT_BUS_DIRECT");
        alt1.setTransportMode("Bus");
        alt1.setRouteName("Bus 15");
        alt1.setEstimatedTimeMinutes(baseDelay + 8);
        alt1.setNumberOfTransfers(0);
        alt1.setWalkingDistanceMeters(250);
        alt1.setDescription("Direct bus service on nearby route");
        alternatives.add(alt1);

        // Alternative 2: Metro
        AlternativeRoute alt2 = new AlternativeRoute();
        alt2.setRouteId("ALT_METRO_1");
        alt2.setTransportMode("Metro");
        alt2.setRouteName("Metro Line 3");
        alt2.setEstimatedTimeMinutes(baseDelay + 12);
        alt2.setNumberOfTransfers(0);
        alt2.setWalkingDistanceMeters(500);
        alt2.setDescription("Metro service with slightly longer walk");
        alternatives.add(alt2);

        // Alternative 3: Walk + Bus
        AlternativeRoute alt3 = new AlternativeRoute();
        alt3.setRouteId("ALT_WALK_BUS");
        alt3.setTransportMode("Walk + Bus");
        alt3.setRouteName("Walk to Bus 22");
        alt3.setEstimatedTimeMinutes(baseDelay + 18);
        alt3.setNumberOfTransfers(0);
        alt3.setWalkingDistanceMeters(800);
        alt3.setDescription("Walk to alternative bus stop");
        alternatives.add(alt3);

        return alternatives;
    }

    private List<AlternativeRoute> generateMetroAlternatives(Disruption disruption) {
        List<AlternativeRoute> alternatives = new ArrayList<>();
        int baseDelay = disruption.getDelayMinutes() != null ? disruption.getDelayMinutes() : 20;

        // Alternative 1: Bus + Tram
        AlternativeRoute alt1 = new AlternativeRoute();
        alt1.setRouteId("ALT_BUS_TRAM");
        alt1.setTransportMode("Bus + Tram");
        alt1.setRouteName("Bus 15 → Tram 7");
        alt1.setEstimatedTimeMinutes(baseDelay + 15);
        alt1.setNumberOfTransfers(1);
        alt1.setWalkingDistanceMeters(300);
        alt1.setDescription("Combined bus and tram service");
        alternatives.add(alt1);

        // Alternative 2: Alternative Metro Line
        AlternativeRoute alt2 = new AlternativeRoute();
        alt2.setRouteId("ALT_METRO_2");
        alt2.setTransportMode("Metro");
        alt2.setRouteName("Metro Line 4 (longer route)");
        alt2.setEstimatedTimeMinutes(baseDelay + 20);
        alt2.setNumberOfTransfers(0);
        alt2.setWalkingDistanceMeters(400);
        alt2.setDescription("Alternative metro line with longer travel time");
        alternatives.add(alt2);

        // Alternative 3: Express Bus
        AlternativeRoute alt3 = new AlternativeRoute();
        alt3.setRouteId("ALT_EXPRESS_BUS");
        alt3.setTransportMode("Bus");
        alt3.setRouteName("Express Bus X1");
        alt3.setEstimatedTimeMinutes(baseDelay + 10);
        alt3.setNumberOfTransfers(0);
        alt3.setWalkingDistanceMeters(600);
        alt3.setDescription("Express bus service");
        alternatives.add(alt3);

        return alternatives;
    }

    private List<AlternativeRoute> generateTrainAlternatives(Disruption disruption) {
        List<AlternativeRoute> alternatives = new ArrayList<>();
        int baseDelay = disruption.getDelayMinutes() != null ? disruption.getDelayMinutes() : 30;

        // Alternative 1: Metro + Bus + Cycle (Dynamic)
        AlternativeRoute alt1 = new AlternativeRoute();
        alt1.setRouteId("ALT_METRO_BUS_CYCLE");
        alt1.setTransportMode("Metro + Cycle");

        // Check for bike stations
        List<CycleStation> bikeStations = cycleStationRepository.findAll();
        String bikeStation = bikeStations.isEmpty() ? "City Bike Station" : bikeStations.get(0).getName();

        alt1.setRouteName("Metro Line 1 -> Bike at " + bikeStation);
        alt1.setEstimatedTimeMinutes(baseDelay + 20);
        alt1.setNumberOfTransfers(1);
        alt1.setWalkingDistanceMeters(500);
        alt1.setDescription("Metro and bike combination to bypass train");
        alternatives.add(alt1);

        // Alternative 2: Alternative Train Line
        AlternativeRoute alt2 = new AlternativeRoute();
        alt2.setRouteId("ALT_TRAIN_2");
        alt2.setTransportMode("Train");
        alt2.setRouteName("Regional Train R2");
        alt2.setEstimatedTimeMinutes(baseDelay + 25);
        alt2.setNumberOfTransfers(0);
        alt2.setWalkingDistanceMeters(700);
        alt2.setDescription("Alternative train line");
        alternatives.add(alt2);

        return alternatives;
    }

    private List<AlternativeRoute> generateGenericAlternatives(Disruption disruption) {
        List<AlternativeRoute> alternatives = new ArrayList<>();
        int baseDelay = disruption.getDelayMinutes() != null ? disruption.getDelayMinutes() : 20;

        // Generic alternative 1
        AlternativeRoute alt1 = new AlternativeRoute();
        alt1.setRouteId("ALT_GENERIC_1");
        alt1.setTransportMode("Mixed");
        alt1.setRouteName("Alternative Route 1");
        alt1.setEstimatedTimeMinutes(baseDelay + 15);
        alt1.setNumberOfTransfers(1);
        alt1.setWalkingDistanceMeters(400);
        alt1.setDescription("Alternative transport combination");
        alternatives.add(alt1);

        // Generic alternative 2
        AlternativeRoute alt2 = new AlternativeRoute();
        alt2.setRouteId("ALT_GENERIC_2");
        alt2.setTransportMode("Mixed");
        alt2.setRouteName("Alternative Route 2");
        alt2.setEstimatedTimeMinutes(baseDelay + 20);
        alt2.setNumberOfTransfers(0);
        alt2.setWalkingDistanceMeters(600);
        alt2.setDescription("Direct alternative with more walking");
        alternatives.add(alt2);

        return alternatives;
    }
}
