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
    log.info("GET /api/v1/ev/charging-stations");
    try {
      return ResponseEntity.ok(evService.getChargingStations());
    } catch (Exception e) {
      log.error("Error fetching EV charging stations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/charging-demand")
  public ResponseEntity<EVChargingDemandResponseDTO> getChargingDemand() {
    log.info("GET /api/v1/ev/charging-demand");
    try {
      return ResponseEntity.ok(evService.getChargingDemand());
    } catch (Exception e) {
      log.error("Error fetching EV charging demand: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/areas-geojson")
  public ResponseEntity<Map<String, Object>> getAreasGeoJson() {
    log.info("GET /api/v1/ev/areas-geojson");
    try {
      return ResponseEntity.ok(evService.getAreasGeoJson());
    } catch (Exception e) {
      log.error("Error fetching EV areas GeoJSON: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
