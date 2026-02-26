package com.trinity.hermes.indicators.bus.controller;

import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
import com.trinity.hermes.indicators.bus.dto.BusSystemPerformanceDTO;
import com.trinity.hermes.indicators.bus.facade.BusFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bus")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BusController {

  private final BusFacade busFacade;

  @GetMapping("/kpis")
  public ResponseEntity<BusDashboardKpiDTO> getKpis() {
    log.info("GET /api/v1/bus/kpis");
    return ResponseEntity.ok(busFacade.getKpis());
  }

  @GetMapping("/live-vehicles")
  public ResponseEntity<List<BusLiveVehicleDTO>> getLiveVehicles() {
    log.info("GET /api/v1/bus/live-vehicles");
    return ResponseEntity.ok(busFacade.getLiveVehiclePositions());
  }

  @GetMapping("/route-utilization")
  public ResponseEntity<List<BusRouteUtilizationDTO>> getRouteUtilization() {
    log.info("GET /api/v1/bus/route-utilization");
    return ResponseEntity.ok(busFacade.getRouteUtilization());
  }

  @GetMapping("/system-performance")
  public ResponseEntity<BusSystemPerformanceDTO> getSystemPerformance() {
    log.info("GET /api/v1/bus/system-performance");
    return ResponseEntity.ok(busFacade.getSystemPerformance());
  }

  @PostMapping("/metrics/refresh")
  public ResponseEntity<Void> refreshMetrics() {
    log.info("POST /api/v1/bus/metrics/refresh");
    busFacade.refreshMetrics();
    return ResponseEntity.ok().build();
  }
}
