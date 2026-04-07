package com.trinity.hermes.indicators.bus.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusNewStopRecommendationDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteDetailDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteShapePointDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteStopDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
import com.trinity.hermes.indicators.bus.dto.BusStopSummaryDTO;
import com.trinity.hermes.indicators.bus.dto.BusSystemPerformanceDTO;
import com.trinity.hermes.indicators.bus.facade.BusFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BusController.class)
@AutoConfigureMockMvc(addFilters = false)
class BusControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private BusFacade busFacade;

  @Test
  void getKpis_returnsOkWithKpiData() throws Exception {
    BusDashboardKpiDTO kpis =
        BusDashboardKpiDTO.builder()
            .totalBusesRunning(25)
            .activeDelays(3)
            .fleetUtilizationPct(85.5)
            .sustainabilityScore(92.0)
            .build();
    when(busFacade.getKpis()).thenReturn(kpis);

    mockMvc
        .perform(get("/api/v1/bus/kpis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalBusesRunning").value(25))
        .andExpect(jsonPath("$.activeDelays").value(3))
        .andExpect(jsonPath("$.fleetUtilizationPct").value(85.5))
        .andExpect(jsonPath("$.sustainabilityScore").value(92.0));
  }

  @Test
  void getLiveVehicles_returnsOkWithVehicleList() throws Exception {
    BusLiveVehicleDTO vehicle =
        BusLiveVehicleDTO.builder()
            .vehicleId(100)
            .routeShortName("42")
            .latitude(53.3)
            .longitude(-6.2)
            .status("on-time")
            .occupancyPct(50.0)
            .delaySeconds(0)
            .build();
    when(busFacade.getLiveVehiclePositions()).thenReturn(List.of(vehicle));

    mockMvc
        .perform(get("/api/v1/bus/live-vehicles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].vehicleId").value(100))
        .andExpect(jsonPath("$[0].routeShortName").value("42"))
        .andExpect(jsonPath("$[0].status").value("on-time"));
  }

  @Test
  void getRouteUtilization_returnsOkWithUtilizationList() throws Exception {
    BusRouteUtilizationDTO utilization =
        BusRouteUtilizationDTO.builder()
            .routeId("route_1")
            .routeShortName("42")
            .routeLongName("City Center - Sandyford")
            .utilizationPct(95.0)
            .activeVehicles(10)
            .status("critical")
            .build();
    when(busFacade.getRouteUtilization()).thenReturn(List.of(utilization));

    mockMvc
        .perform(get("/api/v1/bus/route-utilization"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].routeShortName").value("42"))
        .andExpect(jsonPath("$[0].utilizationPct").value(95.0))
        .andExpect(jsonPath("$[0].status").value("critical"));
  }

  @Test
  void getSystemPerformance_returnsOkWithPerformanceData() throws Exception {
    BusSystemPerformanceDTO performance =
        BusSystemPerformanceDTO.builder().reliabilityPct(88.0).lateArrivalPct(12.0).build();
    when(busFacade.getSystemPerformance()).thenReturn(performance);

    mockMvc
        .perform(get("/api/v1/bus/system-performance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reliabilityPct").value(88.0))
        .andExpect(jsonPath("$.lateArrivalPct").value(12.0));
  }

  @Test
  void getNewStopRecommendations_returnsOkWithList() throws Exception {
    BusNewStopRecommendationDTO row =
        BusNewStopRecommendationDTO.builder()
            .routeId("r1")
            .routeShortName("42")
            .routeLongName("A - B")
            .stopA(
                BusStopSummaryDTO.builder()
                    .id("s1")
                    .code(1)
                    .name("Stop A")
                    .lat(53.0)
                    .lon(-6.2)
                    .build())
            .stopB(
                BusStopSummaryDTO.builder()
                    .id("s2")
                    .code(2)
                    .name("Stop B")
                    .lat(53.1)
                    .lon(-6.3)
                    .build())
            .candidateLat(53.05)
            .candidateLon(-6.25)
            .populationScore(1.0)
            .publicSpaceScore(2.0)
            .combinedScore(4.01)
            .build();
    when(busFacade.getNewStopRecommendations()).thenReturn(List.of(row));

    mockMvc
        .perform(get("/api/v1/bus/new-stops-recommendations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].routeId").value("r1"))
        .andExpect(jsonPath("$[0].combinedScore").value(4.01))
        .andExpect(jsonPath("$[0].stopA.name").value("Stop A"));
  }

  @Test
  void getRouteDetail_returnsOkWithShape() throws Exception {
    BusRouteDetailDTO detail =
        BusRouteDetailDTO.builder()
            .routeId("route_x")
            .agencyId(1)
            .shortName("H2")
            .longName("Docklands - Heuston")
            .representativeTripId("trip_1")
            .shapeId("shape_z")
            .shape(
                List.of(
                    BusRouteShapePointDTO.builder()
                        .sequence(0)
                        .lat(53.35)
                        .lon(-6.26)
                        .distTraveled(0)
                        .build()))
            .stops(
                List.of(
                    BusRouteStopDTO.builder()
                        .sequence(0)
                        .stopId("s1")
                        .code(1)
                        .name("Terminus")
                        .lat(53.35)
                        .lon(-6.26)
                        .build()))
            .build();
    when(busFacade.getRouteDetail("route_x")).thenReturn(detail);

    mockMvc
        .perform(get("/api/v1/bus/routes/route_x"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeId").value("route_x"))
        .andExpect(jsonPath("$.shapeId").value("shape_z"))
        .andExpect(jsonPath("$.representativeTripId").value("trip_1"))
        .andExpect(jsonPath("$.shape[0].lat").value(53.35))
        .andExpect(jsonPath("$.stops[0].stopId").value("s1"))
        .andExpect(jsonPath("$.stops[0].name").value("Terminus"));
  }
}
