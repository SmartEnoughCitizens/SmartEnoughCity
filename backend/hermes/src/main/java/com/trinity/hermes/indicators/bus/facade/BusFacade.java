package com.trinity.hermes.indicators.bus.facade;

import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
import com.trinity.hermes.indicators.bus.dto.BusSystemPerformanceDTO;
import com.trinity.hermes.indicators.bus.service.BusDashboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusFacade {

  private final BusDashboardService busDashboardService;

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
}
