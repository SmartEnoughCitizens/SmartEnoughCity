package com.trinity.hermes.indicators.car.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.hermes.indicators.car.entity.TrafficRecommendation;
import com.trinity.hermes.indicators.car.facade.CarFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrafficRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrafficRecommendationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CarFacade carFacade;

  @Test
  void getTrafficRecommendations_returnsOkWithRecommendationList() throws Exception {
    TrafficRecommendation recommendation =
        TrafficRecommendation.builder()
            .recommendationId("traffic-101-weekday-morning_peak")
            .siteId(101)
            .siteLat(53.34)
            .siteLon(-6.26)
            .title("Diversion plan for Site 101")
            .summary("Critical congestion detected.")
            .dayType("weekday")
            .timeSlot("morning_peak")
            .averageVolume(540.0)
            .congestionLevel("critical")
            .confidenceScore(0.94)
            .recommendedAction("Activate diversion signage.")
            .generatedAt("2026-03-30T09:00:00Z")
            .alternativeRoutes(
                List.of(
                    TrafficRecommendation.AlternativeRoute.builder()
                        .routeId("alt-101-1")
                        .label("North Circular Diversion")
                        .summary("Use primary diversion")
                        .color("#0f766e")
                        .estimatedTimeSavingsMinutes(8)
                        .estimatedTravelTimeMinutes(14)
                        .distanceKm(2.6)
                        .path(
                            List.of(
                                TrafficRecommendation.RouteWaypoint.builder()
                                    .lat(53.336)
                                    .lon(-6.255)
                                    .build()))
                        .build()))
            .build();
    when(carFacade.getTrafficRecommendations()).thenReturn(List.of(recommendation));

    mockMvc
        .perform(get("/api/v1/car/traffic-recommendations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].recommendationId").value("traffic-101-weekday-morning_peak"))
        .andExpect(jsonPath("$[0].siteId").value(101))
        .andExpect(jsonPath("$[0].confidenceScore").value(0.94))
        .andExpect(jsonPath("$[0].alternativeRoutes[0].routeId").value("alt-101-1"))
        .andExpect(jsonPath("$[0].alternativeRoutes[0].estimatedTimeSavingsMinutes").value(8));
  }
}
