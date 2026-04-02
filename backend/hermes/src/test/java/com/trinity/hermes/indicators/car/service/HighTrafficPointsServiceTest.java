package com.trinity.hermes.indicators.car.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HighTrafficPointsServiceTest {

  @Mock private HighTrafficPointsRepository highTrafficPointsRepository;
  @InjectMocks private HighTrafficPointsService highTrafficPointsService;

  // Helper: builds a raw Object[] row matching the MV projection:
  // [site_id, lat, lon, day_type, time_slot, avg_volume]
  private List<Object[]> rows(Object[]... items) {
    return List.of(items);
  }

  private Object[] row(
      int siteId, Double lat, Double lon, String dayType, String timeSlot, double avgVolume) {
    return new Object[] {siteId, lat, lon, dayType, timeSlot, avgVolume};
  }

  @Test
  void getHighTrafficPoints_weekdayMorningPeak_mappedCorrectly() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(101, 53.34, -6.26, "weekday", "morning_peak", 200.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("morning_peak");
    assertThat(result.get(0).getAvgVolume()).isEqualTo(200.0);
  }

  @Test
  void getHighTrafficPoints_weekdayInterPeak_mappedCorrectly() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(102, 53.34, -6.26, "weekday", "inter_peak", 150.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("inter_peak");
  }

  @Test
  void getHighTrafficPoints_weekdayEveningPeak_mappedCorrectly() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(103, 53.35, -6.27, "weekday", "evening_peak", 250.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("evening_peak");
  }

  @Test
  void getHighTrafficPoints_weekdayOffPeak_mappedCorrectly() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(104, 53.35, -6.27, "weekday", "off_peak", 80.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("off_peak");
  }

  @Test
  void getHighTrafficPoints_weekend_mappedCorrectly() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(106, 53.34, -6.26, "weekend", "morning_peak", 120.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekend");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("morning_peak");
  }

  @Test
  void getHighTrafficPoints_weekendInterPeak_mappedCorrectly() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(107, 53.34, -6.26, "weekend", "inter_peak", 90.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekend");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("inter_peak");
  }

  @Test
  void getHighTrafficPoints_differentSites_returnsSeparateDtos() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(
            List.of(
                row(101, 53.34, -6.26, "weekday", "morning_peak", 200.0),
                row(102, 53.35, -6.27, "weekday", "morning_peak", 150.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(2);
  }

  @Test
  void getHighTrafficPoints_sameSiteDifferentSlots_returnsSeparateDtos() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(
            List.of(
                row(101, 53.34, -6.26, "weekday", "morning_peak", 200.0),
                row(101, 53.34, -6.26, "weekday", "inter_peak", 150.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(HighTrafficPointsDTO::getTimeSlot)
        .containsExactlyInAnyOrder("morning_peak", "inter_peak");
  }

  @Test
  void getHighTrafficPoints_avgVolumePassedThrough() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(101, 53.34, -6.26, "weekday", "morning_peak", 250.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAvgVolume()).isEqualTo(250.0);
  }

  @Test
  void getHighTrafficPoints_preservesLocationData() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(101, 53.3498, -6.2603, "weekday", "morning_peak", 200.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result.get(0).getSiteId()).isEqualTo(101);
    assertThat(result.get(0).getLat()).isEqualTo(53.3498);
    assertThat(result.get(0).getLon()).isEqualTo(-6.2603);
  }

  @Test
  void getHighTrafficPoints_nullLatLon_handledGracefully() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(rows(row(101, null, null, "weekday", "off_peak", 50.0)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLat()).isNull();
    assertThat(result.get(0).getLon()).isNull();
  }
}
