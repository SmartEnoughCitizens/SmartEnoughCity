package com.trinity.hermes.indicators.train.service;

import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.dto.TrainDelayDTO;
import com.trinity.hermes.indicators.train.dto.TrainDemandSimulateRequestDTO;
import com.trinity.hermes.indicators.train.dto.TrainDemandSimulateResponseDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainRouteDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
import com.trinity.hermes.indicators.train.dto.TrainStationDemandDTO;
import com.trinity.hermes.indicators.train.entity.TrainCurrentTrain;
import com.trinity.hermes.indicators.train.repository.GtfsDublinStopProjection;
import com.trinity.hermes.indicators.train.repository.GtfsRouteStopProjection;
import com.trinity.hermes.indicators.train.repository.GtfsStopRepository;
import com.trinity.hermes.indicators.train.repository.TrainCurrentTrainRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationDataRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class TrainDashboardService {

  // ── Greater Dublin Area bounding box ─────────────────────────────
  private static final double DUBLIN_LAT_MIN = 53.05;
  private static final double DUBLIN_LAT_MAX = 53.75;
  private static final double DUBLIN_LON_MIN = -6.65;
  private static final double DUBLIN_LON_MAX = -5.90;

  private final TrainStationRepository trainStationRepository;
  private final TrainCurrentTrainRepository trainCurrentTrainRepository;
  private final TrainStationDataRepository trainStationDataRepository;
  private final GtfsStopRepository gtfsStopRepository;
  private final RestTemplate restTemplate;
  private final String inferenceEngineBaseUrl;

  public TrainDashboardService(
      TrainStationRepository trainStationRepository,
      TrainCurrentTrainRepository trainCurrentTrainRepository,
      TrainStationDataRepository trainStationDataRepository,
      GtfsStopRepository gtfsStopRepository,
      RestTemplate restTemplate,
      @Value("${inference-engine.base-url:http://localhost:8000}") String inferenceEngineBaseUrl) {
    this.trainStationRepository = trainStationRepository;
    this.trainCurrentTrainRepository = trainCurrentTrainRepository;
    this.trainStationDataRepository = trainStationDataRepository;
    this.gtfsStopRepository = gtfsStopRepository;
    this.restTemplate = restTemplate;
    this.inferenceEngineBaseUrl = inferenceEngineBaseUrl;
  }

  // ── Stations ─────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TrainDTO> getStations(int limit) {
    // Primary: GTFS train_stops filtered to Dublin
    long gtfsCount = gtfsStopRepository.countDublinStops(
        DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);

    if (gtfsCount > 0) {
      log.debug("Using GTFS stops ({} Dublin stations)", gtfsCount);
      return gtfsStopRepository
          .findDublinStops(DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX)
          .stream()
          .limit(limit)
          .map(this::mapGtfsToTrainDTO)
          .collect(Collectors.toList());
    }

    // Fallback: irish_rail_stations
    log.warn("GTFS stops empty — falling back to irish_rail_stations");
    return trainStationRepository
        .findAllDublinStations(DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX)
        .stream()
        .limit(limit)
        .map(this::mapToTrainDTO)
        .collect(Collectors.toList());
  }

  // ── KPIs ──────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public TrainKpiDTO getKpis() {
    long totalStations = gtfsStopRepository.countDublinStops(
        DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);

    if (totalStations == 0) {
      totalStations = trainStationRepository.countDublinStations(
          DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);
    }

    long liveTrainsRunning = trainCurrentTrainRepository.countActiveDublinTrains(
        DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);
    Double lateArrivalPct = trainStationDataRepository.findLateArrivalPct();
    Double avgDelayMinutes = trainStationDataRepository.findAverageLateMinutes();

    double latePct = lateArrivalPct != null ? lateArrivalPct : 0.0;

    return TrainKpiDTO.builder()
        .totalStations(totalStations)
        .liveTrainsRunning(liveTrainsRunning)
        .onTimePct(Math.max(0.0, 100.0 - latePct))
        .avgDelayMinutes(avgDelayMinutes != null ? avgDelayMinutes : 0.0)
        .build();
  }

  // ── Live trains ───────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TrainLiveDTO> getLiveTrains() {
    return trainCurrentTrainRepository
        .findLatestDublinTrainPositions(
            DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX)
        .stream()
        .map(this::mapToLiveDTO)
        .collect(Collectors.toList());
  }

  // ── Service stats ─────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public TrainServiceStatsDTO getServiceStats() {
    Double lateArrivalPct = trainStationDataRepository.findLateArrivalPct();
    Double avgDueMinutes = trainStationDataRepository.findAverageDueInMinutes();

    double latePct = lateArrivalPct != null ? lateArrivalPct : 0.0;

    return TrainServiceStatsDTO.builder()
        .reliabilityPct(Math.max(0.0, 100.0 - latePct))
        .lateArrivalPct(latePct)
        .avgDueMinutes(avgDueMinutes != null ? avgDueMinutes : 0.0)
        .build();
  }

  // ── Routes ────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TrainRouteDTO> getRoutes() {
    List<GtfsRouteStopProjection> rows = gtfsStopRepository.findRoutePolylines(
        DUBLIN_LAT_MIN, DUBLIN_LAT_MAX, DUBLIN_LON_MIN, DUBLIN_LON_MAX);
    log.info("GTFS route query returned {} Dublin-area stop rows", rows.size());

    // Group by routeId preserving insertion order (already sorted by route+sequence)
    Map<String, List<GtfsRouteStopProjection>> grouped = new LinkedHashMap<>();
    for (GtfsRouteStopProjection row : rows) {
      grouped.computeIfAbsent(row.getRouteId(), k -> new ArrayList<>()).add(row);
    }

    return grouped.values().stream()
        .map(
            stops -> {
              List<GtfsRouteStopProjection> sorted =
                  stops.stream()
                      .sorted(Comparator.comparingInt(
                          p -> p.getLocationOrder() != null ? p.getLocationOrder() : 0))
                      .collect(Collectors.toList());
              List<double[]> coords =
                  sorted.stream()
                      .map(p -> new double[]{p.getLat(), p.getLon()})
                      .collect(Collectors.toList());
              List<String> stopIds =
                  sorted.stream()
                      .map(GtfsRouteStopProjection::getStopId)
                      .collect(Collectors.toList());
              GtfsRouteStopProjection first = stops.get(0);
              return new TrainRouteDTO(first.getRouteName(), first.getShortName(), coords, stopIds);
            })
        .filter(r -> r.getStops().size() >= 2) // drop routes with only one Dublin stop
        .collect(Collectors.toList());
  }

  // ── Delays ────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TrainDelayDTO> getFrequentlyDelayedTrains() {
    return trainStationDataRepository.findFrequentlyDelayedTrains().stream()
        .map(p -> new TrainDelayDTO(
            p.getTrainCode(), p.getOrigin(), p.getDestination(),
            p.getDirection(), p.getTotalAvgDelayMinutes()))
        .collect(Collectors.toList());
  }

  // ── Demand (proxied to inference engine) ─────────────────────────

  public List<TrainStationDemandDTO> getDemand() {
    String url = inferenceEngineBaseUrl + "/train/demand";
    log.info("Fetching train demand scores from inference engine: {}", url);
    TrainStationDemandDTO[] result = restTemplate.getForObject(url, TrainStationDemandDTO[].class);
    return result != null ? Arrays.asList(result) : List.of();
  }

  public TrainDemandSimulateResponseDTO simulateDemand(TrainDemandSimulateRequestDTO request) {
    String url = inferenceEngineBaseUrl + "/train/demand/simulate";
    log.info("Posting demand simulation to inference engine: {}", url);
    // Inference engine expects snake_case — map corridors manually
    List<Map<String, Object>> corridors = request.getCorridors() == null
        ? List.of()
        : request.getCorridors().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("origin_stop_id", c.getOriginStopId());
            m.put("destination_stop_id", c.getDestinationStopId());
            m.put("train_count", c.getTrainCount());
            return m;
          }).collect(Collectors.toList());
    Map<String, Object> body = Map.of("corridors", corridors);
    return restTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(body),
        new ParameterizedTypeReference<TrainDemandSimulateResponseDTO>() {}
    ).getBody();
  }

  // ── Mapping helpers ───────────────────────────────────────────────

  private TrainDTO mapGtfsToTrainDTO(GtfsDublinStopProjection s) {
    TrainDTO dto = new TrainDTO();
    dto.setId(s.getId().hashCode() & Integer.MAX_VALUE);
    dto.setStationCode(s.getId());
    dto.setStationDesc(s.getName());
    dto.setLat(s.getLat());
    dto.setLon(s.getLon());
    dto.setStationType(s.getStationType()); // from LEFT JOIN — may be null
    return dto;
  }

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
