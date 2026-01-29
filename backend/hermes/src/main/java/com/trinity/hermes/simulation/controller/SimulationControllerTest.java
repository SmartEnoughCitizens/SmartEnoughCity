package com.trinity.hermes.simulation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.simulation.dto.RunSimulationRequest;
import com.trinity.hermes.simulation.dto.SimulationResponse;
import com.trinity.hermes.simulation.facade.SimulationFacade;
import com.trinity.hermes.simulation.model.SimulationResults;
import com.trinity.hermes.simulation.model.SimulationSummary;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test-First Tests for SimulationController
 * 
 * These tests define the API contract before implementation.
 */
@WebMvcTest(SimulationController.class)
@DisplayName("SimulationController")
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimulationFacade simulationFacade;

    // =========================================================================
    // TEST DATA FIXTURES
    // =========================================================================

    private SimulationResponse createMockSimulationResponse(Long recommendationId) {
        SimulationSummary summary = new SimulationSummary(71.4, 28.0, 7.0, 70);
        SimulationResults results = new SimulationResults(
            summary,
            Arrays.asList("Optimize traffic lights", "Monitor flow")
        );

        return new SimulationResponse(
            "sim-123",
            recommendationId,
            "Simulation for: " + recommendationId,
            "Impact analysis",
            "recommendation-impact",
            "completed",
            "demo-user",
            LocalDateTime.now(),
            LocalDateTime.now(),
            results
        );
    }

    // =========================================================================
    // CURRENT ENDPOINT TESTS: POST /api/v1/simulations/run
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/simulations/run")
    class RunSimulationEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with simulation results when recommendation exists")
        void shouldReturn200WhenRecommendationExists() throws Exception {
            // Given
            RunSimulationRequest request = new RunSimulationRequest(1L);
            SimulationResponse response = createMockSimulationResponse(1L);
            
            when(simulationFacade.runSimulation(any(RunSimulationRequest.class)))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sim-123"))
                .andExpect(jsonPath("$.recommendationId").value(1))
                .andExpect(jsonPath("$.status").value("completed"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when recommendation doesn't exist")
        void shouldReturn404WhenRecommendationNotFound() throws Exception {
            // Given
            RunSimulationRequest request = new RunSimulationRequest(999L);
            
            when(simulationFacade.runSimulation(any(RunSimulationRequest.class)))
                .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return simulation results with summary metrics")
        void shouldReturnSimulationResultsWithSummary() throws Exception {
            // Given
            RunSimulationRequest request = new RunSimulationRequest(1L);
            SimulationResponse response = createMockSimulationResponse(1L);
            
            when(simulationFacade.runSimulation(any(RunSimulationRequest.class)))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.summary.avgSpeed").value(71.4))
                .andExpect(jsonPath("$.results.summary.congestionLevel").value(28.0))
                .andExpect(jsonPath("$.results.summary.totalDelay").value(7.0))
                .andExpect(jsonPath("$.results.summary.affectedVehicles").value(70));
        }

        @Test
        @DisplayName("Should return simulation results with recommendations list")
        void shouldReturnSimulationResultsWithRecommendations() throws Exception {
            // Given
            RunSimulationRequest request = new RunSimulationRequest(1L);
            SimulationResponse response = createMockSimulationResponse(1L);
            
            when(simulationFacade.runSimulation(any(RunSimulationRequest.class)))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.recommendations").isArray())
                .andExpect(jsonPath("$.results.recommendations[0]").value("Optimize traffic lights"));
        }
    }

    // =========================================================================
    // PHASE 2 ENDPOINT TESTS: POST /api/v1/simulations/bikes
    // These tests will FAIL until the endpoint is implemented
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/simulations/bikes (Phase 2)")
    class BikeSimulationEndpointTests {

        // Uncomment these tests when implementing Phase 2

        /*
        @Test
        @DisplayName("Should simulate adding bikes to a station")
        void shouldSimulateAddingBikes() throws Exception {
            // Given
            BikeSimulationRequest request = new BikeSimulationRequest();
            request.setStationId("grafton-st");
            request.setBikesToAdd(10);

            BikeSimulationResponse response = new BikeSimulationResponse();
            response.setCurrentAvailability(45.0);
            response.setProjectedAvailability(78.0);
            response.setImprovementPercentage(33.0);

            when(simulationFacade.runBikeSimulation(any()))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/bikes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAvailability").value(45.0))
                .andExpect(jsonPath("$.projectedAvailability").value(78.0))
                .andExpect(jsonPath("$.improvementPercentage").value(33.0));
        }

        @Test
        @DisplayName("Should simulate removing bikes from a station")
        void shouldSimulateRemovingBikes() throws Exception {
            // Given
            BikeSimulationRequest request = new BikeSimulationRequest();
            request.setStationId("grafton-st");
            request.setBikesToRemove(5);

            BikeSimulationResponse response = new BikeSimulationResponse();
            response.setCurrentAvailability(78.0);
            response.setProjectedAvailability(60.0);
            response.setImprovementPercentage(-18.0);
            response.setWarnings(Arrays.asList("Availability will drop below 65% threshold"));

            when(simulationFacade.runBikeSimulation(any()))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/bikes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.warnings[0]").exists());
        }

        @Test
        @DisplayName("Should simulate adding a new station")
        void shouldSimulateAddingNewStation() throws Exception {
            // Given
            BikeSimulationRequest request = new BikeSimulationRequest();
            request.setNewStationName("New Station");
            request.setNewStationLatitude(53.3498);
            request.setNewStationLongitude(-6.2603);
            request.setBikesToAdd(20);

            BikeSimulationResponse response = new BikeSimulationResponse();
            response.setNewStationCoverageArea(500); // meters radius
            response.setProjectedDailyUsage(150);

            when(simulationFacade.runBikeSimulation(any()))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/bikes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStationCoverageArea").value(500))
                .andExpect(jsonPath("$.projectedDailyUsage").value(150));
        }

        @Test
        @DisplayName("Should return 400 for invalid bike simulation request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given - missing required fields
            String invalidRequest = "{}";

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/bikes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
        */
    }

    // =========================================================================
    // PHASE 2 ENDPOINT TESTS: POST /api/v1/simulations/buses
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/simulations/buses (Phase 2)")
    class BusSimulationEndpointTests {

        // Uncomment these tests when implementing Phase 2

        /*
        @Test
        @DisplayName("Should simulate changing bus frequency")
        void shouldSimulateChangingBusFrequency() throws Exception {
            // Given
            BusSimulationRequest request = new BusSimulationRequest();
            request.setRouteId("46A");
            request.setFrequencyChangePercent(20); // +20%

            BusSimulationResponse response = new BusSimulationResponse();
            response.setCurrentAvgWaitTime(12.0); // minutes
            response.setProjectedAvgWaitTime(10.0);
            response.setProjectedCongestionReduction(15.0);

            when(simulationFacade.runBusSimulation(any()))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/buses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAvgWaitTime").value(12.0))
                .andExpect(jsonPath("$.projectedAvgWaitTime").value(10.0));
        }

        @Test
        @DisplayName("Should simulate route modification")
        void shouldSimulateRouteModification() throws Exception {
            // Given
            BusSimulationRequest request = new BusSimulationRequest();
            request.setRouteId("46A");
            request.setStopsToAdd(Arrays.asList("new-stop-1", "new-stop-2"));

            BusSimulationResponse response = new BusSimulationResponse();
            response.setAdditionalTravelTime(5); // minutes
            response.setNewPassengersServed(200);

            when(simulationFacade.runBusSimulation(any()))
                .thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/buses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalTravelTime").value(5))
                .andExpect(jsonPath("$.newPassengersServed").value(200));
        }
        */
    }

    // =========================================================================
    // PHASE 2 ENDPOINT TESTS: POST /api/v1/simulations/compare
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/simulations/compare (Phase 2)")
    class CompareSimulationsEndpointTests {

        // Uncomment these tests when implementing Phase 2

        /*
        @Test
        @DisplayName("Should compare multiple scenarios")
        void shouldCompareMultipleScenarios() throws Exception {
            // Given
            CompareSimulationsRequest request = new CompareSimulationsRequest();
            request.setScenarios(Arrays.asList(
                new ScenarioRequest("Add 10 bikes", "BIKE", Map.of("bikesToAdd", 10)),
                new ScenarioRequest("Add 20 bikes", "BIKE", Map.of("bikesToAdd", 20)),
                new ScenarioRequest("Add new station", "BIKE", Map.of("newStation", true))
            ));

            CompareSimulationsResponse response = new CompareSimulationsResponse();
            response.setBestScenario("Add 20 bikes");
            response.setScenarioResults(/* ... */);

            when(simulationFacade.compareSimulations(any()))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/simulations/compare")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestScenario").value("Add 20 bikes"))
                .andExpect(jsonPath("$.scenarioResults").isArray());
        }
        */
    }
}
