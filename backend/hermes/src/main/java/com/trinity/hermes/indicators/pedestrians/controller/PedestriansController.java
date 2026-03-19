package com.trinity.hermes.indicators.pedestrians.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.pedestrians.dto.PedestrianLiveDTO;
import com.trinity.hermes.indicators.pedestrians.facade.PedestriansFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PedestriansController {

  private final PedestriansFacade pedestriansFacade;

  @GetMapping("/api/v1/pedestrians/live")
  public ResponseEntity<List<PedestrianLiveDTO>> getLivePedestrianCounts(
      @RequestParam(defaultValue = "20") Integer limit) {
    log.info("GET /api/v1/pedestrians/live?limit={}", LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(pedestriansFacade.getLiveCounts(limit));
    } catch (Exception e) {
      log.error("Error fetching pedestrian live counts: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
