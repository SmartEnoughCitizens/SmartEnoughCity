package com.trinity.hermes.indicators.train.service;

import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.dto.TrainDelayPatternDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
import com.trinity.hermes.indicators.train.dto.TrainStationUtilizationDTO;
import com.trinity.hermes.indicators.train.entity.TrainCurrentTrain;
import com.trinity.hermes.indicators.train.repository.TrainCurrentTrainRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationDataRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainDashboardService {

  // ── Greater Dublin Area bounding box ─────────────────────────────
  // Covers: Greystones (south) → Drogheda (north), Maynooth (west) → coast (east)
  private static final double DUBLIN_LAT_MIN = 53.05;
  private static final double DUBLIN_LAT_MAX = 53.75;
  private static final double DUBLIN_LON_MIN = -6.65;
  private static final double DUBLIN_LON_MAX = -5.90;

  private final TrainStationRepository trainStationRepository;
  private final TrainCurrentTrainRepository trainCurrentTrainRepository;
  private final TrainStationDataRepository trainStationDataRepository;

  @Transactional(readOnly = true)
  public List<TrainDTO> getStations(int limit) {
    log.debug("Fetching up to {} Dublin-area train stations", limit);
    return trainStationRepository
        .findAllDublinStations(DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX)
        .stream()
        .limit(limit)
        .map(this::mapToTrainDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public TrainKpiDTO getKpis() {
    log.info("Fetching Dublin train dashboard KPIs");

    long totalStations =
        trainStationRepository.countDublinStations(
            DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);
    long liveTrainsRunning =
        trainCurrentTrainRepository.countActiveDublinTrains(
            DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);
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
    log.info("Fetching live Dublin train positions");
    return trainCurrentTrainRepository
        .findLatestDublinTrainPositions(
            DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX)
        .stream()
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

  // ── Utilization ──────────────────────────────────────────────────

  /**
   * Returns per-station utilization for the Greater Dublin Area.
   *
   * <p>Each station's {@code trainServiceCount} is the number of active train services in the
   * current snapshot. Stations are classified HIGH / MEDIUM / LOW relative to the Dublin mean:
   * HIGH &gt; 150 % of mean, LOW &lt; 50 % of mean, MEDIUM otherwise.
   */
  @Transactional(readOnly = true)
  public List<TrainStationUtilizationDTO> getStationUtilization() {
    log.info("Fetching train station utilization");

    List<Object[]> rows =
        trainStationDataRepository.findStationUtilization(
            DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);

    // Build DTOs with raw counts first
    List<TrainStationUtilizationDTO> dtos =
        rows.stream()
            .map(
                r -> {
                  String code = (String) r[0];
                  String desc = (String) r[1];
                  double lat = ((Number) r[2]).doubleValue();
                  double lon = ((Number) r[3]).doubleValue();
                  long svcCount = ((Number) r[4]).longValue();
                  double avgDelay = ((Number) r[5]).doubleValue();
                  return TrainStationUtilizationDTO.builder()
                      .stationCode(code)
                      .stationDesc(desc)
                      .lat(lat)
                      .lon(lon)
                      .trainServiceCount(svcCount)
                      .avgDelayMinutes(avgDelay)
                      .build();
                })
            .collect(Collectors.toList());

    // Compute Dublin mean service count, then classify
    OptionalDouble meanOpt =
        dtos.stream().mapToLong(TrainStationUtilizationDTO::getTrainServiceCount).average();
    double mean = meanOpt.orElse(1.0);

    dtos.forEach(
        dto -> {
          double ratio = dto.getTrainServiceCount() / mean;
          String level;
          if (ratio > 1.5) {
            level = "HIGH";
          } else if (ratio < 0.5) {
            level = "LOW";
          } else {
            level = "MEDIUM";
          }
          dto.setUtilizationLevel(level);
        });

    return dtos;
  }

  // ── Delay patterns ────────────────────────────────────────────────

  /**
   * Returns recurring delay patterns for the Greater Dublin Area over the given historical window.
   *
   * <p>Each pattern represents a (station, origin→destination route, train type, time-of-day)
   * combination whose average delay is at least 1 minute. Results are sorted worst-first and capped
   * at 200 rows by the underlying query.
   *
   * @param days number of calendar days to look back (e.g. 7, 30, 90)
   */
  @Transactional(readOnly = true)
  public List<TrainDelayPatternDTO> getDelayPatterns(int days) {
    log.info("Fetching train delay patterns for the last {} days", days);

    LocalDate fromDate = LocalDate.now(ZoneId.of("Europe/Dublin")).minusDays(days);

    List<Object[]> rows =
        trainStationDataRepository.findDelayPatterns(
            fromDate, DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);

    return rows.stream()
        .map(
            r -> {
              String stationCode = (String) r[0];
              String stationDesc = (String) r[1];
              double lat = ((Number) r[2]).doubleValue();
              double lon = ((Number) r[3]).doubleValue();
              String origin = (String) r[4];
              String destination = (String) r[5];
              String trainType = (String) r[6];
              String timeOfDay = (String) r[7];
              double avgDelay = ((Number) r[8]).doubleValue();
              int maxDelay = ((Number) r[9]).intValue();
              long occurrences = ((Number) r[10]).longValue();
              double latePct = ((Number) r[11]).doubleValue();

              String severity;
              if (avgDelay >= 10.0) {
                severity = "SEVERE";
              } else if (avgDelay >= 5.0) {
                severity = "MODERATE";
              } else {
                severity = "MINOR";
              }

              return TrainDelayPatternDTO.builder()
                  .stationCode(stationCode)
                  .stationDesc(stationDesc)
                  .lat(lat)
                  .lon(lon)
                  .origin(origin)
                  .destination(destination)
                  .trainType(trainType)
                  .timeOfDay(timeOfDay)
                  .avgDelayMinutes(avgDelay)
                  .maxDelayMinutes(maxDelay)
                  .occurrenceCount(occurrences)
                  .latePercent(latePct)
                  .severityLevel(severity)
                  .build();
            })
        .collect(Collectors.toList());
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
