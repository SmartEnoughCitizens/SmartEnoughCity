package com.trinity.hermes.indicators.ev.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.ev.dto.EVAreaDemandDTO;
import com.trinity.hermes.indicators.ev.dto.EVChargingDemandResponseDTO;
import com.trinity.hermes.indicators.ev.dto.EVChargingStationsResponseDTO;
import com.trinity.hermes.indicators.ev.dto.EVStationDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class EVServiceTest {

  @Mock private RestTemplate restTemplate;

  private EVService evService;

  private static final String BASE_URL = "http://localhost:8000";

  @BeforeEach
  void setUp() {
    evService = new EVService(restTemplate, BASE_URL);
  }

  @Test
  void getChargingStations_returnsResponseFromInferenceEngine() {
    EVStationDTO station = new EVStationDTO();
    station.setAddress("O'Connell Street");
    station.setCounty("Dublin");
    station.setChargerCount(2);

    EVChargingStationsResponseDTO expected = new EVChargingStationsResponseDTO();
    expected.setTotalStations(1);
    expected.setStations(List.of(station));

    when(restTemplate.getForObject(
            BASE_URL + "/ev/charging-stations", EVChargingStationsResponseDTO.class))
        .thenReturn(expected);

    EVChargingStationsResponseDTO result = evService.getChargingStations();

    assertThat(result.getTotalStations()).isEqualTo(1);
    assertThat(result.getStations()).hasSize(1);
    assertThat(result.getStations().get(0).getAddress()).isEqualTo("O'Connell Street");
  }

  @Test
  void getChargingStations_whenInferenceEngineReturnsNull_returnsNull() {
    when(restTemplate.getForObject(
            BASE_URL + "/ev/charging-stations", EVChargingStationsResponseDTO.class))
        .thenReturn(null);

    EVChargingStationsResponseDTO result = evService.getChargingStations();

    assertThat(result).isNull();
  }

  @Test
  void getChargingDemand_returnsResponseFromInferenceEngine() {
    EVAreaDemandDTO area = new EVAreaDemandDTO();
    area.setArea("Arran Quay A, Dublin City");
    area.setRegisteredEvs(217);
    area.setChargingDemand(4);

    EVChargingDemandResponseDTO expected = new EVChargingDemandResponseDTO();
    expected.setSummary(Map.of("total_areas", 1, "high_priority_count", 0));
    expected.setHighPriorityAreas(List.of());
    expected.setAreas(List.of(area));

    when(restTemplate.getForObject(
            BASE_URL + "/ev/charging-demand", EVChargingDemandResponseDTO.class))
        .thenReturn(expected);

    EVChargingDemandResponseDTO result = evService.getChargingDemand();

    assertThat(result.getAreas()).hasSize(1);
    assertThat(result.getAreas().get(0).getArea()).isEqualTo("Arran Quay A, Dublin City");
    assertThat(result.getHighPriorityAreas()).isEmpty();
  }

  @Test
  void getChargingDemand_whenInferenceEngineReturnsNull_returnsNull() {
    when(restTemplate.getForObject(
            BASE_URL + "/ev/charging-demand", EVChargingDemandResponseDTO.class))
        .thenReturn(null);

    EVChargingDemandResponseDTO result = evService.getChargingDemand();

    assertThat(result).isNull();
  }

  @Test
  void getAreasGeoJson_returnsMapFromInferenceEngine() {
    Map<String, Object> expected = Map.of("type", "FeatureCollection", "features", List.of());

    when(restTemplate.getForObject(BASE_URL + "/ev/areas-geojson", Map.class)).thenReturn(expected);

    Map<String, Object> result = evService.getAreasGeoJson();

    assertThat(result).containsEntry("type", "FeatureCollection");
    assertThat(result.get("features")).isInstanceOf(List.class);
  }

  @Test
  void getAreasGeoJson_whenInferenceEngineThrows_propagatesException() {
    when(restTemplate.getForObject(BASE_URL + "/ev/areas-geojson", Map.class))
        .thenThrow(new RuntimeException("Connection refused"));

    assertThatThrownBy(() -> evService.getAreasGeoJson())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Connection refused");
  }
}
