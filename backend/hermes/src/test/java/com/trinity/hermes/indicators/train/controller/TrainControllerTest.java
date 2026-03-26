package com.trinity.hermes.indicators.train.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.dto.TrainDelayDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
import com.trinity.hermes.indicators.train.facade.TrainFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrainController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrainControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private TrainFacade trainFacade;

  // ── Test 1 ────────────────────────────────────────────────────────

  @Test
  void getTrainData_returnsOkWithStationList() throws Exception {
    TrainDTO station = new TrainDTO(1, "DART1", "Connolly", null, 53.35, -6.25, "D");
    when(trainFacade.getStations(200)).thenReturn(List.of(station));

    mockMvc
        .perform(get("/api/v1/dashboard/train"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicatorType").value("train"))
        .andExpect(jsonPath("$.totalRecords").value(1))
        .andExpect(jsonPath("$.data[0].stationCode").value("DART1"))
        .andExpect(jsonPath("$.data[0].stationDesc").value("Connolly"));
  }

  // ── Test 2 ────────────────────────────────────────────────────────

  @Test
  void getKpis_returnsOkWithKpiData() throws Exception {
    TrainKpiDTO kpis =
        TrainKpiDTO.builder()
            .totalStations(50)
            .liveTrainsRunning(12)
            .onTimePct(88.5)
            .avgDelayMinutes(2.3)
            .build();
    when(trainFacade.getKpis()).thenReturn(kpis);

    mockMvc
        .perform(get("/api/v1/train/kpis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalStations").value(50))
        .andExpect(jsonPath("$.liveTrainsRunning").value(12))
        .andExpect(jsonPath("$.onTimePct").value(88.5))
        .andExpect(jsonPath("$.avgDelayMinutes").value(2.3));
  }

  // ── Test 3 ────────────────────────────────────────────────────────

  @Test
  void getLiveTrains_returnsOkWithLiveTrainList() throws Exception {
    TrainLiveDTO train =
        TrainLiveDTO.builder()
            .trainCode("E801")
            .direction("Northbound")
            .trainType("DART")
            .status("R")
            .lat(53.35)
            .lon(-6.25)
            .build();
    when(trainFacade.getLiveTrains()).thenReturn(List.of(train));

    mockMvc
        .perform(get("/api/v1/train/live-trains"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].trainCode").value("E801"))
        .andExpect(jsonPath("$[0].direction").value("Northbound"))
        .andExpect(jsonPath("$[0].status").value("R"));
  }

  // ── Test 4 ────────────────────────────────────────────────────────

  @Test
  void getServiceStats_returnsOkWithStats() throws Exception {
    TrainServiceStatsDTO stats =
        TrainServiceStatsDTO.builder()
            .reliabilityPct(88.0)
            .lateArrivalPct(12.0)
            .avgDueMinutes(4.5)
            .build();
    when(trainFacade.getServiceStats()).thenReturn(stats);

    mockMvc
        .perform(get("/api/v1/train/service-stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reliabilityPct").value(88.0))
        .andExpect(jsonPath("$.lateArrivalPct").value(12.0))
        .andExpect(jsonPath("$.avgDueMinutes").value(4.5));
  }

  // ── Test 5 ────────────────────────────────────────────────────────

  @Test
  void getFrequentlyDelayedTrains_returnsOkWithDelayList() throws Exception {
    TrainDelayDTO delay = new TrainDelayDTO("E801", "Greystones", "Malahide", "Northbound", 12.45);
    when(trainFacade.getFrequentlyDelayedTrains()).thenReturn(List.of(delay));

    mockMvc
        .perform(get("/api/v1/train/frequent-delays"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].trainCode").value("E801"))
        .andExpect(jsonPath("$[0].origin").value("Greystones"))
        .andExpect(jsonPath("$[0].destination").value("Malahide"))
        .andExpect(jsonPath("$[0].direction").value("Northbound"))
        .andExpect(jsonPath("$[0].totalAvgDelayMinutes").value(12.45));
  }

  // ── Test 6 ────────────────────────────────────────────────────────

  @Test
  void getFrequentlyDelayedTrains_whenServiceThrows_returns500() throws Exception {
    doThrow(new RuntimeException("DB unavailable")).when(trainFacade).getFrequentlyDelayedTrains();

    mockMvc
        .perform(get("/api/v1/train/frequent-delays"))
        .andExpect(status().isInternalServerError());
  }
}
