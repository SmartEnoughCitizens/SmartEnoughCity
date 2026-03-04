package com.trinity.hermes.indicators.train.service;

import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
import com.trinity.hermes.indicators.train.entity.TrainCurrentTrain;
import com.trinity.hermes.indicators.train.repository.TrainCurrentTrainRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationDataRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainDashboardService {

  private final TrainStationRepository trainStationRepository;
  private final TrainCurrentTrainRepository trainCurrentTrainRepository;
  private final TrainStationDataRepository trainStationDataRepository;

  @Transactional(readOnly = true)
  public List<TrainDTO> getStations(int limit) {
    log.debug("Fetching up to {} train stations", limit);
    return trainStationRepository.findAll(PageRequest.of(0, limit)).stream()
        .map(this::mapToTrainDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public TrainKpiDTO getKpis() {
    log.info("Fetching train dashboard KPIs");

    long totalStations = trainStationRepository.count();
    long liveTrainsRunning = trainCurrentTrainRepository.countActiveTrains();
    Double lateArrivalPct = trainStationDataRepository.findLateArrivalPct();
    Double avgDelayMinutes = trainStationDataRepository.findAverageLateMinutes();

    double latePct = lateArrivalPct != null ? lateArrivalPct : 0.0;
    double onTimePct = Math.max(0.0, 100.0 - latePct);

    return TrainKpiDTO.builder()
        .totalStations(totalStations)
        .liveTrainsRunning(liveTrainsRunning)
        .onTimePct(onTimePct)
        .avgDelayMinutes(avgDelayMinutes != null ? avgDelayMinutes : 0.0)
        .build();
  }

  @Transactional(readOnly = true)
  public List<TrainLiveDTO> getLiveTrains() {
    log.info("Fetching live train positions");
    return trainCurrentTrainRepository.findLatestPositionPerTrain().stream()
        .map(this::mapToLiveDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public TrainServiceStatsDTO getServiceStats() {
    log.info("Fetching train service stats");

    Double lateArrivalPct = trainStationDataRepository.findLateArrivalPct();
    Double avgDueMinutes = trainStationDataRepository.findAverageDueInMinutes();

    double latePct = lateArrivalPct != null ? lateArrivalPct : 0.0;
    double reliabilityPct = Math.max(0.0, 100.0 - latePct);

    return TrainServiceStatsDTO.builder()
        .reliabilityPct(reliabilityPct)
        .lateArrivalPct(latePct)
        .avgDueMinutes(avgDueMinutes != null ? avgDueMinutes : 0.0)
        .build();
  }

  // ── Mapping helpers ──────────────────────────────────────────────

  private TrainDTO mapToTrainDTO(com.trinity.hermes.indicators.train.entity.TrainStation entity) {
    TrainDTO dto = new TrainDTO();
    dto.setId(entity.getId());
    dto.setStationCode(entity.getStationCode());
    dto.setStationDesc(entity.getStationDesc());
    dto.setStationAlias(entity.getStationAlias());
    dto.setLat(entity.getLat());
    dto.setLon(entity.getLon());
    dto.setStationType(entity.getStationType());
    return dto;
  }

  private TrainLiveDTO mapToLiveDTO(TrainCurrentTrain t) {
    return TrainLiveDTO.builder()
        .trainCode(t.getTrainCode())
        .direction(t.getDirection())
        .trainType(t.getTrainType())
        .status(t.getTrainStatus())
        .lat(t.getLat())
        .lon(t.getLon())
        .publicMessage(t.getPublicMessage())
        .build();
  }
}
