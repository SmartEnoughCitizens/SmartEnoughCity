package com.trinity.hermes.indicators.bus.service;

import com.trinity.hermes.mv.service.MaterializedViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusMetricsComputeService {

  private static final String MV_NAME = "bus_route_metrics";

  private final MaterializedViewService materializedViewService;

  /**
   * Triggers a refresh of the bus_route_metrics materialized view.
   * Scheduling is now managed by MvSchedulerService — the cron is stored in mv_registry
   * and configured via POST /api/v1/mv when the MV is registered.
   * This method is kept for BusFacade compatibility (POST /api/v1/bus/metrics/refresh).
   */
  public void computeMetrics() {
    materializedViewService.refresh(MV_NAME);
  }
}
