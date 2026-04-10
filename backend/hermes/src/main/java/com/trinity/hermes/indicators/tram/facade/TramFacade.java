package com.trinity.hermes.indicators.tram.facade;

import com.trinity.hermes.indicators.tram.dto.*;
import com.trinity.hermes.indicators.tram.service.TramDashboardService;
import com.trinity.hermes.recommendation.dto.RecommendationResponse;
import com.trinity.hermes.recommendation.service.RecommendationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TramFacade {

  private final TramDashboardService tramDashboardService;
  private final RecommendationService recommendationService;

  public List<TramStopDTO> getStops(int limit) {
    return tramDashboardService.getStops(limit);
  }

  public TramKpiDTO getKpis() {
    return tramDashboardService.getKpis();
  }

  public List<TramLiveForecastDTO> getLiveForecasts() {
    return tramDashboardService.getLiveForecasts();
  }

  public List<TramAlternativeRouteDTO> getAlternativeRoutes(String stopId) {
    return tramDashboardService.getAlternativeRoutes(stopId);
  }

  public List<TramDelayDTO> getDelays() {
    return tramDashboardService.getDelays();
  }

  public List<TramHourlyDistributionDTO> getHourlyDistribution() {
    return tramDashboardService.getHourlyDistribution();
  }

  public List<TramStopUsageDTO> getStopUsage(int startHour, int endHour) {
    return tramDashboardService.getStopUsage(startHour, endHour);
  }

  public List<TramCommonDelayDTO> getCommonDelays() {
    return tramDashboardService.getCommonDelays();
  }

  public List<TramStopDemandDTO> getStopDemand() {
    return tramDashboardService.getStopDemand();
  }

  public TramDemandSimulateResponseDTO simulateDemand(TramDemandSimulateRequestDTO request) {
    return tramDashboardService.simulateDemand(request);
  }

  public List<RecommendationResponse> getRecommendations() {
    return recommendationService.getRecommendationsByIndicator("Tram");
  }
}
