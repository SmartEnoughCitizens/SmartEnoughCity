package com.trinity.hermes.indicators.bus.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
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
  void refreshMetrics_returnsOk() throws Exception {
    mockMvc.perform(post("/api/v1/bus/metrics/refresh")).andExpect(status().isOk());

    verify(busFacade).refreshMetrics();
  }
}
