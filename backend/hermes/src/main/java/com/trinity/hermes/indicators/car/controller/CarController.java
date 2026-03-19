package com.trinity.hermes.indicators.car.controller;

import com.trinity.hermes.indicators.car.dto.CarDashboardDTO;
import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.facade.CarFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/car")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CarController {

  private final CarFacade carFacade;

  @GetMapping("/fuel-type-statistics")
  public ResponseEntity<List<CarDashboardDTO>> getFuelTypeStatistics() {
    log.info("GET /api/v1/car/fuel-type-statistics");
    return ResponseEntity.ok(carFacade.getFuelTypeStatistics());
  }

  @GetMapping("/high-traffic-points")
  public ResponseEntity<List<HighTrafficPointsDTO>> getHighTrafficPoints() {
    log.info("GET /api/v1/car/high-traffic-points");
    return ResponseEntity.ok(carFacade.getHighTrafficPoints());
  }
}
