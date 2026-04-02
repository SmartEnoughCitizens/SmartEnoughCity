package com.trinity.hermes.disruptionmanagement.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeDTO;
import com.trinity.hermes.disruptionmanagement.dto.CauseDTO;
import com.trinity.hermes.disruptionmanagement.dto.DisruptionResponse;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({DisruptionController.class, PublicDisruptionController.class})
@AutoConfigureMockMvc(addFilters = false)
class DisruptionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DisruptionFacade disruptionFacade;

  // ── GET /api/v1/disruptions/{id} ────────────────────────────────────

  @Test
  void getById_found_returns200WithCausesAndAlternatives() throws Exception {
    DisruptionResponse response =
        DisruptionResponse.builder()
            .id(1L)
            .name("Bus Delay — Route 46A")
            .severity("HIGH")
            .disruptionType("DELAY")
            .affectedArea("Dublin 2")
            .affectedTransportModes(List.of("BUS"))
            .causes(
                List.of(
                    CauseDTO.builder()
                        .id(10L)
                        .causeType("EVENT")
                        .causeDescription("Large event at Aviva")
                        .confidence("HIGH")
                        .build()))
            .alternatives(
                List.of(
                    AlternativeDTO.builder()
                        .id(20L)
                        .mode("rail")
                        .stopName("Connolly")
                        .description("Irish Rail: Connolly (200m away)")
                        .build()))
            .build();

    when(disruptionFacade.getDisruptionById(1L)).thenReturn(Optional.of(response));

    mockMvc
        .perform(get("/api/v1/disruptions/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.severity").value("HIGH"))
        .andExpect(jsonPath("$.causes[0].causeType").value("EVENT"))
        .andExpect(jsonPath("$.causes[0].confidence").value("HIGH"))
        .andExpect(jsonPath("$.alternatives[0].mode").value("rail"))
        .andExpect(jsonPath("$.alternatives[0].stopName").value("Connolly"));
  }

  @Test
  void getById_notFound_returns404() throws Exception {
    when(disruptionFacade.getDisruptionById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/disruptions/99")).andExpect(status().isNotFound());
  }

  // ── GET /api/v1/disruptions/transport/{mode} ────────────────────────

  @Test
  void getByTransportMode_returnsFilteredList() throws Exception {
    DisruptionResponse tram =
        DisruptionResponse.builder()
            .id(2L)
            .severity("MEDIUM")
            .affectedTransportModes(List.of("TRAM"))
            .build();

    when(disruptionFacade.getActiveDisruptionsByTransportMode("TRAM")).thenReturn(List.of(tram));

    mockMvc
        .perform(get("/api/v1/disruptions/transport/TRAM"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(2))
        .andExpect(jsonPath("$[0].affectedTransportModes[0]").value("TRAM"));
  }

  @Test
  void getByTransportMode_noMatches_returnsEmptyList() throws Exception {
    when(disruptionFacade.getActiveDisruptionsByTransportMode("TRAIN")).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/disruptions/transport/TRAIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ── GET /api/public/disruptions/{id} ────────────────────────────────

  @Test
  void publicGetById_found_returns200() throws Exception {
    DisruptionResponse response =
        DisruptionResponse.builder()
            .id(5L)
            .name("Tram disruption")
            .severity("HIGH")
            .affectedTransportModes(List.of("TRAM"))
            .causes(List.of())
            .alternatives(List.of())
            .build();

    when(disruptionFacade.getDisruptionById(5L)).thenReturn(Optional.of(response));

    mockMvc
        .perform(get("/api/public/disruptions/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.name").value("Tram disruption"));
  }

  @Test
  void publicGetById_notFound_returns404() throws Exception {
    when(disruptionFacade.getDisruptionById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/public/disruptions/99")).andExpect(status().isNotFound());
  }
}
