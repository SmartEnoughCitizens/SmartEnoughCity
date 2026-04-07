package com.trinity.hermes.indicators.car.controller;

import com.trinity.hermes.indicators.car.entity.TrafficRecommendation;
import com.trinity.hermes.indicators.car.facade.CarFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/car")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TrafficRecommendationController {

  private final CarFacade carFacade;

  @GetMapping("/traffic-recommendations")
  public ResponseEntity<List<TrafficRecommendation>> getTrafficRecommendations() {
    log.info("GET /api/v1/car/traffic-recommendations");
    return ResponseEntity.ok(carFacade.getTrafficRecommendations());
  }

  @PostMapping("/traffic-recommendations/{recommendationId}/notify")
  public ResponseEntity<Void> notifyRecommendation(@PathVariable String recommendationId) {
    log.info("POST /api/v1/car/traffic-recommendations/{}/notify", recommendationId);
    return carFacade
        .notifyRecommendation(recommendationId)
        .<ResponseEntity<Void>>map(r -> ResponseEntity.ok().build())
        .orElse(ResponseEntity.notFound().build());
  }
}
