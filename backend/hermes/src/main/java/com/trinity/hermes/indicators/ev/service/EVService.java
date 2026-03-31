package com.trinity.hermes.indicators.ev.service;

import com.trinity.hermes.indicators.ev.dto.EVChargingDemandResponseDTO;
import com.trinity.hermes.indicators.ev.dto.EVChargingStationsResponseDTO;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class EVService {

  private final RestTemplate restTemplate;
  private final String inferenceEngineBaseUrl;

  public EVService(
      RestTemplate restTemplate,
      @Value("${inference-engine.base-url:http://localhost:8000}") String inferenceEngineBaseUrl) {
    this.restTemplate = restTemplate;
    this.inferenceEngineBaseUrl = inferenceEngineBaseUrl;
  }

  public EVChargingStationsResponseDTO getChargingStations() {
    String url = inferenceEngineBaseUrl + "/ev/charging-stations";
    log.info("Fetching EV charging stations from inference engine: {}", url);
    return restTemplate.getForObject(url, EVChargingStationsResponseDTO.class);
  }

  public EVChargingDemandResponseDTO getChargingDemand() {
    String url = inferenceEngineBaseUrl + "/ev/charging-demand";
    log.info("Fetching EV charging demand from inference engine: {}", url);
    return restTemplate.getForObject(url, EVChargingDemandResponseDTO.class);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getAreasGeoJson() {
    String url = inferenceEngineBaseUrl + "/ev/areas-geojson";
    log.info("Fetching EV areas GeoJSON from inference engine: {}", url);
    return restTemplate.getForObject(url, Map.class);
  }
}
