package com.trinity.hermes.indicators.car.facade;

import com.trinity.hermes.indicators.car.dto.CarDashboardDTO;
import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.dto.JunctionEmissionDTO;
import com.trinity.hermes.indicators.car.entity.TrafficRecommendation;
import com.trinity.hermes.indicators.car.service.CarDashboardService;
import com.trinity.hermes.indicators.car.service.HighTrafficPointsService;
import com.trinity.hermes.indicators.car.service.PollutionEstimationService;
import com.trinity.hermes.indicators.car.service.TrafficCongestionNotifier;
import com.trinity.hermes.indicators.car.service.TrafficRecommendationService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarFacade {

  private final CarDashboardService carDashboardService;
  private final HighTrafficPointsService highTrafficPointsService;
  private final PollutionEstimationService pollutionEstimationService;
  private final TrafficRecommendationService trafficRecommendationService;
  private final TrafficCongestionNotifier trafficCongestionNotifier;

  public List<CarDashboardDTO> getFuelTypeStatistics() {
    return carDashboardService.getFuelTypeStatistics();
  }

  public List<HighTrafficPointsDTO> getHighTrafficPoints() {
    return highTrafficPointsService.getHighTrafficPoints();
  }

  public List<JunctionEmissionDTO> getJunctionEmissions() {
    return pollutionEstimationService.computeEmissions();
  }

  public List<TrafficRecommendation> getTrafficRecommendations() {
    return trafficRecommendationService.getTrafficRecommendations();
  }

  public Optional<TrafficRecommendation> notifyRecommendation(String recommendationId) {
    Optional<TrafficRecommendation> rec =
        trafficRecommendationService.getRecommendationById(recommendationId);
    rec.ifPresent(trafficCongestionNotifier::notifyForRecommendation);
    return rec;
  }
}
