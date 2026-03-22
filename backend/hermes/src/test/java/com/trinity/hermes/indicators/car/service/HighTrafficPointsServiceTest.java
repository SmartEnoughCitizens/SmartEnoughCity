package com.trinity.hermes.indicators.car.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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

  // Helper: builds a raw Object[] row matching the repository query projection:
  // [site_id, end_time, total_volume, lat, lon]
  private Object[] row(int siteId, LocalDateTime dt, double volume, Double lat, Double lon) {
    return new Object[] {siteId, Timestamp.valueOf(dt), volume, lat, lon};
  }

  // --- Time slot classification ---

  @Test
  void getHighTrafficPoints_weekdayAt0730_classifiedAsMorningPeak() {
    // Monday 2024-01-15 07:30 → weekday, morning_peak (07:00–09:59)
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(101, LocalDateTime.of(2024, 1, 15, 7, 30), 200.0, 53.34, -6.26)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("morning_peak");
  }

  @Test
  void getHighTrafficPoints_weekdayAt1200_classifiedAsInterPeak() {
    // Monday 2024-01-15 12:00 → weekday, inter_peak (10:00–15:59)
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(102, LocalDateTime.of(2024, 1, 15, 12, 0), 150.0, 53.34, -6.26)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("inter_peak");
  }

  @Test
  void getHighTrafficPoints_weekdayAt1700_classifiedAsEveningPeak() {
    // Monday 2024-01-15 17:00 → weekday, evening_peak (16:00–18:59)
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(103, LocalDateTime.of(2024, 1, 15, 17, 0), 250.0, 53.35, -6.27)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("evening_peak");
  }

  @Test
  void getHighTrafficPoints_weekdayAt2200_classifiedAsOffPeak() {
    // Monday 2024-01-15 22:00 → weekday, off_peak (19:00+)
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(104, LocalDateTime.of(2024, 1, 15, 22, 0), 80.0, 53.35, -6.27)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekday");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("off_peak");
  }

  @Test
  void getHighTrafficPoints_earlyMorningAt0300_classifiedAsOffPeak() {
    // Monday 2024-01-15 03:00 → weekday, off_peak (before 07:00)
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(105, LocalDateTime.of(2024, 1, 15, 3, 0), 30.0, 53.34, -6.26)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTimeSlot()).isEqualTo("off_peak");
  }

  // --- Day type classification ---

  @Test
  void getHighTrafficPoints_saturday_classifiedAsWeekend() {
    // Saturday 2024-01-13 09:00 → weekend
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(106, LocalDateTime.of(2024, 1, 13, 9, 0), 120.0, 53.34, -6.26)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekend");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("morning_peak");
  }

  @Test
  void getHighTrafficPoints_sunday_classifiedAsWeekend() {
    // Sunday 2024-01-14 14:00 → weekend, inter_peak
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(107, LocalDateTime.of(2024, 1, 14, 14, 0), 90.0, 53.34, -6.26)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDayType()).isEqualTo("weekend");
    assertThat(result.get(0).getTimeSlot()).isEqualTo("inter_peak");
  }

  // --- Grouping and averaging ---

  @Test
  void getHighTrafficPoints_twoRowsSameSiteAndSlot_volumeIsAveraged() {
    // Both rows: site 101, weekday, morning_peak → avg = (200 + 300) / 2 = 250
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(
            List.<Object[]>of(
                row(101, LocalDateTime.of(2024, 1, 15, 7, 30), 200.0, 53.34, -6.26),
                row(101, LocalDateTime.of(2024, 1, 22, 7, 30), 300.0, 53.34, -6.26)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAvgVolume()).isEqualTo(250.0);
  }

  @Test
  void getHighTrafficPoints_differentSites_returnsSeparateDtos() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(
            List.<Object[]>of(
                row(101, LocalDateTime.of(2024, 1, 15, 7, 30), 200.0, 53.34, -6.26),
                row(102, LocalDateTime.of(2024, 1, 15, 7, 30), 150.0, 53.35, -6.27)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(2);
  }

  @Test
  void getHighTrafficPoints_sameSiteDifferentSlots_returnsSeparateDtos() {
    // Site 101 has a morning_peak row and an inter_peak row → two separate DTOs
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(
            List.<Object[]>of(
                row(101, LocalDateTime.of(2024, 1, 15, 8, 0), 200.0, 53.34, -6.26),
                row(101, LocalDateTime.of(2024, 1, 15, 11, 0), 150.0, 53.34, -6.26)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result).hasSize(2);
    assertThat(result).extracting(HighTrafficPointsDTO::getTimeSlot)
        .containsExactlyInAnyOrder("morning_peak", "inter_peak");
  }

  @Test
  void getHighTrafficPoints_preservesLocationData() {
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(row(101, LocalDateTime.of(2024, 1, 15, 9, 0), 200.0, 53.3498, -6.2603)));

    List<HighTrafficPointsDTO> result = highTrafficPointsService.getHighTrafficPoints();

    assertThat(result.get(0).getSiteId()).isEqualTo(101);
    assertThat(result.get(0).getLat()).isEqualTo(53.3498);
    assertThat(result.get(0).getLon()).isEqualTo(-6.2603);
  }
}
