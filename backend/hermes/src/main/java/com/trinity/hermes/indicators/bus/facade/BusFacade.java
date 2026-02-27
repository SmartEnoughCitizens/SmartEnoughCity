package com.trinity.hermes.indicators.bus.facade;

import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
import com.trinity.hermes.indicators.bus.dto.BusSystemPerformanceDTO;
import com.trinity.hermes.indicators.bus.service.BusDashboardService;
import com.trinity.hermes.indicators.bus.service.BusMetricsComputeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusFacade {

  private final BusDashboardService busDashboardService;
  private final BusMetricsComputeService busMetricsComputeService;

  public BusDashboardKpiDTO getKpis() {
    return busDashboardService.getKpis();
  }

  public List<BusLiveVehicleDTO> getLiveVehiclePositions() {
    return busDashboardService.getLiveVehiclePositions();
  }

  public List<BusRouteUtilizationDTO> getRouteUtilization() {
    return busDashboardService.getRouteUtilization();
  }

  public BusSystemPerformanceDTO getSystemPerformance() {
    return busDashboardService.getSystemPerformance();
  }

  public void refreshMetrics() {
    log.info("Triggering metrics refresh via facade");
    busMetricsComputeService.computeMetrics();
  }
}
