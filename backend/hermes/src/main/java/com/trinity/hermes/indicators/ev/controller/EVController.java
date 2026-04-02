package com.trinity.hermes.indicators.ev.controller;

import com.trinity.hermes.indicators.ev.dto.EVChargingDemandResponseDTO;
import com.trinity.hermes.indicators.ev.dto.EVChargingStationsResponseDTO;
import com.trinity.hermes.indicators.ev.service.EVService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ev")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EVController {

  private final EVService evService;

  @GetMapping("/charging-stations")
  public ResponseEntity<EVChargingStationsResponseDTO> getChargingStations() {
    log.info("Fetching EV charging stations");
    try {
      EVChargingStationsResponseDTO result = evService.getChargingStations();
      if (result == null) {
        log.error("Inference engine returned null for charging stations");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
      }
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Failed to fetch EV charging stations", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/charging-demand")
  public ResponseEntity<EVChargingDemandResponseDTO> getChargingDemand() {
    log.info("Fetching EV charging demand data");
    try {
      EVChargingDemandResponseDTO result = evService.getChargingDemand();
      if (result == null) {
        log.error("Inference engine returned null for charging demand");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
      }
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Failed to fetch EV charging demand data", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/areas-geojson")
  public ResponseEntity<Map<String, Object>> getAreasGeoJson() {
    log.info("Fetching EV areas GeoJSON");
    try {
      Map<String, Object> result = evService.getAreasGeoJson();
      if (result == null) {
        log.error("Inference engine returned null for areas GeoJSON");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
      }
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Failed to fetch EV areas GeoJSON", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
