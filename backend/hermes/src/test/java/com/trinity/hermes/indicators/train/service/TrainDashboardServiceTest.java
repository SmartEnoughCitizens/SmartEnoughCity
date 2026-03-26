package com.trinity.hermes.indicators.train.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.train.dto.TrainDelayDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
import com.trinity.hermes.indicators.train.entity.TrainCurrentTrain;
import com.trinity.hermes.indicators.train.entity.TrainStation;
import com.trinity.hermes.indicators.train.repository.TrainCurrentTrainRepository;
import com.trinity.hermes.indicators.train.repository.TrainDelayProjection;
import com.trinity.hermes.indicators.train.repository.TrainStationDataRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainDashboardServiceTest {

  @Mock private TrainStationRepository trainStationRepository;
  @Mock private TrainCurrentTrainRepository trainCurrentTrainRepository;
  @Mock private TrainStationDataRepository trainStationDataRepository;

  @InjectMocks private TrainDashboardService trainDashboardService;

  // ── Test 1 ────────────────────────────────────────────────────────

  @Test
  void getKpis_returnsAggregatedKpis() {
    when(trainStationRepository.countDublinStations(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(50L);
    when(trainCurrentTrainRepository.countActiveDublinTrains(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(12L);
    when(trainStationDataRepository.findLateArrivalPct()).thenReturn(15.0);
    when(trainStationDataRepository.findAverageLateMinutes()).thenReturn(3.2);

    TrainKpiDTO kpis = trainDashboardService.getKpis();

    assertThat(kpis.getTotalStations()).isEqualTo(50L);
    assertThat(kpis.getLiveTrainsRunning()).isEqualTo(12L);
    assertThat(kpis.getOnTimePct()).isEqualTo(85.0);
    assertThat(kpis.getAvgDelayMinutes()).isEqualTo(3.2);
  }

  // ── Test 2 ────────────────────────────────────────────────────────

  @Test
  void getKpis_withNullRepositoryValues_returnsZeroDefaults() {
    when(trainStationRepository.countDublinStations(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(0L);
    when(trainCurrentTrainRepository.countActiveDublinTrains(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(0L);
    when(trainStationDataRepository.findLateArrivalPct()).thenReturn(null);
    when(trainStationDataRepository.findAverageLateMinutes()).thenReturn(null);

    TrainKpiDTO kpis = trainDashboardService.getKpis();

    assertThat(kpis.getOnTimePct()).isEqualTo(100.0);
    assertThat(kpis.getAvgDelayMinutes()).isEqualTo(0.0);
  }

  // ── Test 3 ────────────────────────────────────────────────────────

  @Test
  void getKpis_onTimePctIsComplementOfLateArrivalPct() {
    when(trainStationRepository.countDublinStations(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(10L);
    when(trainCurrentTrainRepository.countActiveDublinTrains(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(5L);
    when(trainStationDataRepository.findLateArrivalPct()).thenReturn(30.0);
    when(trainStationDataRepository.findAverageLateMinutes()).thenReturn(5.0);

    TrainKpiDTO kpis = trainDashboardService.getKpis();

    assertThat(kpis.getOnTimePct()).isEqualTo(70.0);
    assertThat(kpis.getOnTimePct()).isGreaterThanOrEqualTo(0.0);
  }

  // ── Test 4 ────────────────────────────────────────────────────────

  @Test
  void getLiveTrains_returnsMappedDtos() {
    TrainCurrentTrain train =
        new TrainCurrentTrain(
            1,
            "E801",
            LocalDate.now(ZoneId.of("UTC")),
            "R",
            "DART",
            "Northbound",
            53.35,
            -6.25,
            "On time",
            LocalDateTime.now(ZoneId.of("UTC")));

    when(trainCurrentTrainRepository.findLatestDublinTrainPositions(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(train));

    List<TrainLiveDTO> result = trainDashboardService.getLiveTrains();

    assertThat(result).hasSize(1);
    TrainLiveDTO dto = result.get(0);
    assertThat(dto.getTrainCode()).isEqualTo("E801");
    assertThat(dto.getDirection()).isEqualTo("Northbound");
    assertThat(dto.getStatus()).isEqualTo("R");
    assertThat(dto.getLat()).isEqualTo(53.35);
    assertThat(dto.getLon()).isEqualTo(-6.25);
  }

  // ── Test 5 ────────────────────────────────────────────────────────

  @Test
  void getServiceStats_returnsCorrectReliabilityAndLatePct() {
    when(trainStationDataRepository.findLateArrivalPct()).thenReturn(12.0);
    when(trainStationDataRepository.findAverageDueInMinutes()).thenReturn(4.5);

    TrainServiceStatsDTO stats = trainDashboardService.getServiceStats();

    assertThat(stats.getReliabilityPct()).isEqualTo(88.0);
    assertThat(stats.getLateArrivalPct()).isEqualTo(12.0);
    assertThat(stats.getAvgDueMinutes()).isEqualTo(4.5);
  }

  // ── Test 6 ────────────────────────────────────────────────────────

  @Test
  void getServiceStats_withNullValues_returnsZeroDefaults() {
    when(trainStationDataRepository.findLateArrivalPct()).thenReturn(null);
    when(trainStationDataRepository.findAverageDueInMinutes()).thenReturn(null);

    TrainServiceStatsDTO stats = trainDashboardService.getServiceStats();

    assertThat(stats.getLateArrivalPct()).isEqualTo(0.0);
    assertThat(stats.getReliabilityPct()).isEqualTo(100.0);
    assertThat(stats.getAvgDueMinutes()).isEqualTo(0.0);
  }

  // ── Test 7 ────────────────────────────────────────────────────────

  @Test
  void getFrequentlyDelayedTrains_returnsMappedDtos() {
    TrainDelayProjection projection =
        new TrainDelayProjection() {
          @Override
          public String getTrainCode() {
            return "E801";
          }

          @Override
          public String getOrigin() {
            return "Greystones";
          }

          @Override
          public String getDestination() {
            return "Malahide";
          }

          @Override
          public String getDirection() {
            return "Northbound";
          }

          @Override
          public Double getTotalAvgDelayMinutes() {
            return 12.45;
          }
        };

    when(trainStationDataRepository.findFrequentlyDelayedTrains()).thenReturn(List.of(projection));

    List<TrainDelayDTO> result = trainDashboardService.getFrequentlyDelayedTrains();

    assertThat(result).hasSize(1);
    TrainDelayDTO dto = result.get(0);
    assertThat(dto.getTrainCode()).isEqualTo("E801");
    assertThat(dto.getOrigin()).isEqualTo("Greystones");
    assertThat(dto.getDestination()).isEqualTo("Malahide");
    assertThat(dto.getDirection()).isEqualTo("Northbound");
    assertThat(dto.getTotalAvgDelayMinutes()).isEqualTo(12.45);
  }

  // ── Test 8 ────────────────────────────────────────────────────────

  @Test
  void getFrequentlyDelayedTrains_withEmptyRepository_returnsEmptyList() {
    when(trainStationDataRepository.findFrequentlyDelayedTrains()).thenReturn(List.of());

    List<TrainDelayDTO> result = trainDashboardService.getFrequentlyDelayedTrains();

    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  // ── Test 9 ────────────────────────────────────────────────────────

  @Test
  void getStations_appliesLimitCorrectly() {
    TrainStation s1 = new TrainStation(1, "DART1", "Connolly", null, 53.35, -6.25, "D");
    TrainStation s2 = new TrainStation(2, "DART2", "Pearse", null, 53.34, -6.24, "D");
    TrainStation s3 = new TrainStation(3, "DART3", "Lansdowne", null, 53.33, -6.23, "D");

    when(trainStationRepository.findAllDublinStations(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(s1, s2, s3));

    List<?> result = trainDashboardService.getStations(2);

    assertThat(result).hasSize(2);
  }
}
