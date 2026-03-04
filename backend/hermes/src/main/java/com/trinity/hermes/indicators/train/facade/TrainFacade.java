package com.trinity.hermes.indicators.train.facade;

import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
import com.trinity.hermes.indicators.train.service.TrainDashboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainFacade {

  private final TrainDashboardService trainDashboardService;

  public List<TrainDTO> getStations(int limit) {
    return trainDashboardService.getStations(limit);
  }

  public TrainKpiDTO getKpis() {
    return trainDashboardService.getKpis();
  }

  public List<TrainLiveDTO> getLiveTrains() {
    return trainDashboardService.getLiveTrains();
  }

  public TrainServiceStatsDTO getServiceStats() {
    return trainDashboardService.getServiceStats();
  }
}
