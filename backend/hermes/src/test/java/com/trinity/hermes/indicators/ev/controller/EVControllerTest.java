package com.trinity.hermes.indicators.ev.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.hermes.indicators.ev.dto.EVAreaDemandDTO;
import com.trinity.hermes.indicators.ev.dto.EVChargingDemandResponseDTO;
import com.trinity.hermes.indicators.ev.dto.EVChargingStationsResponseDTO;
import com.trinity.hermes.indicators.ev.dto.EVStationDTO;
import com.trinity.hermes.indicators.ev.service.EVService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EVController.class)
@AutoConfigureMockMvc(addFilters = false)
class EVControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private EVService evService;

  @Test
  void getChargingStations_returnsOkWithStationData() throws Exception {
    EVStationDTO station = new EVStationDTO();
    station.setAddress("O'Connell Street");
    station.setCounty("Dublin");
    station.setLatitude(53.3498);
    station.setLongitude(-6.2603);
    station.setChargerCount(2);
    station.setOpenHours("24 x 7");

    EVChargingStationsResponseDTO response = new EVChargingStationsResponseDTO();
    response.setTotalStations(1);
    response.setStations(List.of(station));

    when(evService.getChargingStations()).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/ev/charging-stations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_stations").value(1))
        .andExpect(jsonPath("$.stations[0].address").value("O'Connell Street"))
        .andExpect(jsonPath("$.stations[0].county").value("Dublin"))
        .andExpect(jsonPath("$.stations[0].latitude").value(53.3498))
        .andExpect(jsonPath("$.stations[0].longitude").value(-6.2603))
        .andExpect(jsonPath("$.stations[0].charger_count").value(2))
        .andExpect(jsonPath("$.stations[0].open_hours").value("24 x 7"));
  }

  @Test
  void getChargingStations_whenServiceThrows_returnsInternalServerError() throws Exception {
    when(evService.getChargingStations()).thenThrow(new RuntimeException("Inference engine down"));

    mockMvc
        .perform(get("/api/v1/ev/charging-stations"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void getChargingDemand_returnsOkWithDemandData() throws Exception {
    EVAreaDemandDTO area = new EVAreaDemandDTO();
    area.setArea("Arran Quay A, Dublin City");
    area.setRegisteredEvs(217);
    area.setChargingDemand(4);
    area.setHomeChargePercentage(0.60);
    area.setChargeFrequency(0.067);

    EVChargingDemandResponseDTO response = new EVChargingDemandResponseDTO();
    response.setSummary(Map.of("total_areas", 1, "high_priority_count", 0));
    response.setHighPriorityAreas(List.of());
    response.setAreas(List.of(area));

    when(evService.getChargingDemand()).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/ev/charging-demand"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.total_areas").value(1))
        .andExpect(jsonPath("$.areas[0].area").value("Arran Quay A, Dublin City"))
        .andExpect(jsonPath("$.areas[0].registered_evs").value(217))
        .andExpect(jsonPath("$.areas[0].charging_demand").value(4));
  }

  @Test
  void getChargingDemand_whenServiceThrows_returnsInternalServerError() throws Exception {
    when(evService.getChargingDemand()).thenThrow(new RuntimeException("Inference engine down"));

    mockMvc.perform(get("/api/v1/ev/charging-demand")).andExpect(status().isInternalServerError());
  }

  @Test
  void getAreasGeoJson_returnsOkWithGeoJson() throws Exception {
    Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features", List.of());

    when(evService.getAreasGeoJson()).thenReturn(geoJson);

    mockMvc
        .perform(get("/api/v1/ev/areas-geojson"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("FeatureCollection"));
  }

  @Test
  void getAreasGeoJson_whenServiceThrows_returnsInternalServerError() throws Exception {
    when(evService.getAreasGeoJson()).thenThrow(new RuntimeException("Inference engine down"));

    mockMvc.perform(get("/api/v1/ev/areas-geojson")).andExpect(status().isInternalServerError());
  }
}
