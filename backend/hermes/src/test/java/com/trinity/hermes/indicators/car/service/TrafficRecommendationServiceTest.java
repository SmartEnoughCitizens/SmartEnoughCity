package com.trinity.hermes.indicators.car.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.entity.TrafficRecommendation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrafficRecommendationServiceTest {

  @Mock private HighTrafficPointsService highTrafficPointsService;

  @InjectMocks private TrafficRecommendationService trafficRecommendationService;

  @Test
  void getTrafficRecommendations_returnsRankedRecommendationsWithConfidence() {
    when(highTrafficPointsService.getHighTrafficPoints())
        .thenReturn(
            List.of(
                HighTrafficPointsDTO.builder()
                    .siteId(101)
                    .lat(53.34)
                    .lon(-6.26)
                    .avgVolume(540.0)
                    .dayType("weekday")
                    .timeSlot("morning_peak")
                    .build(),
                HighTrafficPointsDTO.builder()
                    .siteId(202)
                    .lat(53.35)
                    .lon(-6.27)
                    .avgVolume(420.0)
                    .dayType("weekday")
                    .timeSlot("evening_peak")
                    .build()));

    List<TrafficRecommendation> result = trafficRecommendationService.getTrafficRecommendations();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getSiteId()).isEqualTo(101);
    assertThat(result.get(0).getConfidenceScore()).isGreaterThanOrEqualTo(0.68);
    assertThat(result.get(0).getAlternativeRoutes()).hasSize(2);
    assertThat(result.get(0).getAlternativeRoutes().get(0).getEstimatedTimeSavingsMinutes())
        .isGreaterThan(0);
    assertThat(result.get(0).getAlternativeRoutes().get(0).getPath()).hasSize(4);
  }

  @Test
  void getTrafficRecommendations_withNoTrafficData_returnsEmptyList() {
    when(highTrafficPointsService.getHighTrafficPoints()).thenReturn(List.of());

    List<TrafficRecommendation> result = trafficRecommendationService.getTrafficRecommendations();

    assertThat(result).isEmpty();
  }
}
