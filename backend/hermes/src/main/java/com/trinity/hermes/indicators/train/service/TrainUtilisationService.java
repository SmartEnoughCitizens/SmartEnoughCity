package com.trinity.hermes.indicators.train.service;

import com.trinity.hermes.indicators.train.dto.TrainSimulateResponseDTO;
import com.trinity.hermes.indicators.train.dto.TrainUtilisationResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class TrainUtilisationService {

  private final RestTemplate restTemplate;
  private final String inferenceEngineBaseUrl;

  public TrainUtilisationService(
      RestTemplate restTemplate,
      @Value("${inference-engine.base-url:http://localhost:8000}") String inferenceEngineBaseUrl) {
    this.restTemplate = restTemplate;
    this.inferenceEngineBaseUrl = inferenceEngineBaseUrl;
  }

  public TrainUtilisationResponseDTO getUtilisation() {
    String url = inferenceEngineBaseUrl + "/train/utilisation";
    log.info("Fetching train utilisation from inference engine: {}", url);
    return restTemplate.getForObject(url, TrainUtilisationResponseDTO.class);
  }

  public TrainSimulateResponseDTO runSimulation() {
    String url = inferenceEngineBaseUrl + "/train/utilisation/simulate";
    log.info("Triggering train utilisation simulation: {}", url);
    return restTemplate.postForObject(url, null, TrainSimulateResponseDTO.class);
  }
}
