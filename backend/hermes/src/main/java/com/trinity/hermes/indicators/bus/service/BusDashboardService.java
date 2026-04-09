package com.trinity.hermes.indicators.bus.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.bus.dto.BusCommonDelayDTO;
import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusNewStopRecommendationDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteBreakdownDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteDetailDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteShapePointDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteStopDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
import com.trinity.hermes.indicators.bus.dto.BusStopSummaryDTO;
import com.trinity.hermes.indicators.bus.dto.BusSystemPerformanceDTO;
import com.trinity.hermes.indicators.bus.entity.BusLiveVehicle;
import com.trinity.hermes.indicators.bus.entity.BusRidership;
import com.trinity.hermes.indicators.bus.entity.BusRoute;
import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import com.trinity.hermes.indicators.bus.entity.BusStop;
import com.trinity.hermes.indicators.bus.entity.BusStopTime;
import com.trinity.hermes.indicators.bus.entity.BusTrip;
import com.trinity.hermes.indicators.bus.entity.BusTripShape;
import com.trinity.hermes.indicators.bus.repository.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusDashboardService {

  private static final int DELAY_THRESHOLD_SECONDS = 120;

  private final BusLiveVehicleRepository busLiveVehicleRepository;
  private final BusLiveStopTimeUpdateRepository busLiveStopTimeUpdateRepository;
  private final BusRouteMetricsRepository busRouteMetricsRepository;
  private final BusRidershipRepository busRidershipRepository;
  private final BusTripRepository busTripRepository;
  private final BusTripShapeRepository busTripShapeRepository;
  private final BusStopTimeRepository busStopTimeRepository;
  private final BusStopRepository busStopRepository;
  private final BusRouteRepository busRouteRepository;
  private final BusCommonDelayMvRepository busCommonDelayMvRepository;
  private final BusNewStopRecommendationsRepository busNewStopRecommendationsRepository;

  @Transactional(readOnly = true)
  public BusDashboardKpiDTO getKpis() {
    log.info("Fetching dashboard KPIs");

    Long totalBuses = busLiveVehicleRepository.countActiveVehicles();
    Long activeDelays = busLiveStopTimeUpdateRepository.countActiveDelays(DELAY_THRESHOLD_SECONDS);
    Double avgUtilization = busRouteMetricsRepository.findFleetUtilization();
    Double avgReliability = busRouteMetricsRepository.findAverageReliability();

    double sustainabilityScore =
        computeSustainabilityScore(avgReliability != null ? avgReliability : 0.0);

    return BusDashboardKpiDTO.builder()
        .totalBusesRunning(totalBuses != null ? totalBuses : 0L)
        .activeDelays(activeDelays != null ? activeDelays : 0L)
        .fleetUtilizationPct(avgUtilization != null ? avgUtilization : 0.0)
        .sustainabilityScore(sustainabilityScore)
        .build();
  }

  @Transactional(readOnly = true)
  public List<BusLiveVehicleDTO> getLiveVehiclePositions() {
    log.info("Fetching live vehicle positions");

    List<BusLiveVehicle> vehicles = busLiveVehicleRepository.findRecentVehicles();
    if (vehicles.isEmpty()) {
      return List.of();
    }

    Set<String> tripIds =
        vehicles.stream().map(BusLiveVehicle::getTripId).collect(Collectors.toSet());
    Map<String, BusTrip> tripsById =
        busTripRepository.findAllById(tripIds).stream()
            .collect(Collectors.toMap(BusTrip::getId, Function.identity()));

    Set<String> routeIds =
        tripsById.values().stream().map(BusTrip::getRouteId).collect(Collectors.toSet());
    Map<String, BusRoute> routesById =
        busRouteRepository.findAllById(routeIds).stream()
            .collect(Collectors.toMap(BusRoute::getId, Function.identity()));

    Map<Integer, BusRidership> ridershipByVehicleId =
        busRidershipRepository.findRecentPerVehicle().stream()
            .collect(Collectors.toMap(BusRidership::getVehicleId, Function.identity()));

    return vehicles.stream()
        .map(v -> mapToLiveVehicleDTO(v, tripsById, routesById, ridershipByVehicleId))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<BusRouteUtilizationDTO> getRouteUtilization() {
    log.info("Fetching route utilization");

    List<BusRouteMetrics> allMetrics = busRouteMetricsRepository.findAll();

    return allMetrics.stream()
        .map(this::mapToRouteUtilizationDTO)
        .sorted(java.util.Comparator.comparingDouble(BusRouteUtilizationDTO::getUtilizationPct))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public BusSystemPerformanceDTO getSystemPerformance() {
    log.info("Fetching system performance");

    Double avgReliability = busRouteMetricsRepository.findAverageReliability();
    Double avgLateArrival = busRouteMetricsRepository.findAverageLateArrival();

    return BusSystemPerformanceDTO.builder()
        .reliabilityPct(avgReliability != null ? avgReliability : 0.0)
        .lateArrivalPct(avgLateArrival != null ? avgLateArrival : 0.0)
        .build();
  }

  @Transactional(readOnly = true)
  public List<BusCommonDelayDTO> getCommonDelays(String filter) {
    log.info("Fetching common bus delays from MV, filter={}", LogSanitizer.sanitizeLog(filter));
    String safeFilter = List.of("today", "week", "month").contains(filter) ? filter : "today";
    return busCommonDelayMvRepository.findByPeriodOrderByAvgDelayMinutesDesc(safeFilter).stream()
        .map(
            mv ->
                BusCommonDelayDTO.builder()
                    .routeId(mv.getRouteId())
                    .routeShortName(mv.getRouteShortName())
                    .routeLongName(mv.getRouteLongName())
                    .avgDelayMinutes(mv.getAvgDelayMinutes())
                    .build())
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public BusRouteDetailDTO getRouteDetail(String routeId) {
    log.info("Fetching bus route detail with shape, routeId={}", routeId);
    BusRoute route =
        busRouteRepository
            .findById(routeId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Route not found: " + routeId));
    BusTrip trip =
        busTripRepository
            .findFirstByRouteIdOrderByIdAsc(routeId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No trips found for route: " + routeId));

    List<BusRouteShapePointDTO> shapePoints =
        busTripShapeRepository.findByShapeIdOrderByPtSequenceAsc(trip.getShapeId()).stream()
            .map(this::mapShapePoint)
            .collect(Collectors.toList());

    List<BusStopTime> stopTimes =
        busStopTimeRepository.findByTripIdOrderBySequenceAsc(trip.getId());
    Set<String> stopIds =
        stopTimes.stream().map(BusStopTime::getStopId).collect(Collectors.toSet());
    Map<String, BusStop> stopsById =
        busStopRepository.findAllById(stopIds).stream()
            .collect(Collectors.toMap(BusStop::getId, Function.identity()));
    List<BusRouteStopDTO> stops =
        stopTimes.stream()
            .map(st -> mapRouteStop(st, stopsById.get(st.getStopId())))
            .collect(Collectors.toList());

    return BusRouteDetailDTO.builder()
        .routeId(route.getId())
        .agencyId(route.getAgencyId())
        .shortName(route.getShortName())
        .longName(route.getLongName())
        .representativeTripId(trip.getId())
        .shapeId(trip.getShapeId())
        .shape(shapePoints)
        .stops(stops)
        .build();
  }

  private static BusRouteStopDTO mapRouteStop(BusStopTime st, BusStop stop) {
    BusRouteStopDTO.BusRouteStopDTOBuilder b =
        BusRouteStopDTO.builder()
            .sequence(st.getSequence())
            .stopId(st.getStopId())
            .headsign(st.getHeadsign());
    if (stop != null) {
      b.code(stop.getCode()).name(stop.getName()).lat(stop.getLat()).lon(stop.getLon());
    }
    return b.build();
  }

  private BusRouteShapePointDTO mapShapePoint(BusTripShape s) {
    return BusRouteShapePointDTO.builder()
        .sequence(s.getPtSequence())
        .lat(s.getPtLat())
        .lon(s.getPtLon())
        .distTraveled(s.getDistTraveled())
        .build();
  }

  @Transactional(readOnly = true)
  public List<BusNewStopRecommendationDTO> getNewStopRecommendations() {
    log.info("Fetching top new stop recommendations from MV");
    return busNewStopRecommendationsRepository.findTop20ByCombinedScoreDesc().stream()
        .map(this::mapToNewStopRecommendationDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<BusRouteBreakdownDTO> getRouteBreakdown(String routeId, String filter) {
    log.info(
        "Fetching bus route breakdown, routeId={}, filter={}",
        LogSanitizer.sanitizeLog(routeId),
        LogSanitizer.sanitizeLog(filter));
    String safeFilter = List.of("today", "week", "month").contains(filter) ? filter : "today";
    return busLiveStopTimeUpdateRepository.findBreakdownByRoute(routeId, safeFilter).stream()
        .map(
            p ->
                BusRouteBreakdownDTO.builder()
                    .stopId(p.getStopId())
                    .avgDelayMinutes(p.getAvgDelayMinutes())
                    .maxDelayMinutes(p.getMaxDelayMinutes())
                    .tripCount(p.getTripCount())
                    .build())
        .collect(Collectors.toList());
  }

  private BusLiveVehicleDTO mapToLiveVehicleDTO(
      BusLiveVehicle vehicle,
      Map<String, BusTrip> tripsById,
      Map<String, BusRoute> routesById,
      Map<Integer, BusRidership> ridershipByVehicleId) {
    String routeShortName = "";
    BusTrip trip = tripsById.get(vehicle.getTripId());
    if (trip != null) {
      BusRoute route = routesById.get(trip.getRouteId());
      if (route != null) {
        routeShortName = route.getShortName();
      }
    }

    BusRidership ridership = ridershipByVehicleId.get(vehicle.getVehicleId());

    double occupancyPct = 0.0;
    if (ridership != null && ridership.getVehicleCapacity() > 0) {
      occupancyPct =
          (double) ridership.getPassengersOnboard() / ridership.getVehicleCapacity() * 100.0;
    }

    int delaySeconds = 0;
    String status = "on-time";

    return BusLiveVehicleDTO.builder()
        .vehicleId(vehicle.getVehicleId())
        .routeShortName(routeShortName)
        .latitude(vehicle.getLat())
        .longitude(vehicle.getLon())
        .status(status)
        .occupancyPct(occupancyPct)
        .delaySeconds(delaySeconds)
        .build();
  }

  private BusRouteUtilizationDTO mapToRouteUtilizationDTO(BusRouteMetrics metrics) {
    return BusRouteUtilizationDTO.builder()
        .routeId(metrics.getRouteId())
        .routeShortName(metrics.getRouteShortName())
        .routeLongName(metrics.getRouteLongName())
        .utilizationPct(metrics.getUtilizationPct() != null ? metrics.getUtilizationPct() : 0.0)
        .activeVehicles(metrics.getActiveVehicles() != null ? metrics.getActiveVehicles() : 0)
        .status(
            determineStatus(
                metrics.getUtilizationPct() != null ? metrics.getUtilizationPct() : 0.0))
        .build();
  }

  private String determineStatus(double utilizationPct) {
    if (utilizationPct > 90.0) {
      return "critical";
    } else if (utilizationPct > 80.0) {
      return "high";
    } else if (utilizationPct < 30.0) {
      return "low";
    } else {
      return "normal";
    }
  }

  private double computeSustainabilityScore(double reliabilityPct) {
    return Math.min(100.0, reliabilityPct * 1.05);
  }

  private BusNewStopRecommendationDTO mapToNewStopRecommendationDTO(
      BusNewStopRecommendationProjection p) {
    return BusNewStopRecommendationDTO.builder()
        .routeId(p.getRouteId())
        .routeShortName(p.getRouteShortName())
        .routeLongName(p.getRouteLongName())
        .stopA(
            BusStopSummaryDTO.builder()
                .id(p.getStopAId())
                .code(p.getStopACode())
                .name(p.getStopAName())
                .lat(p.getStopALat())
                .lon(p.getStopALon())
                .build())
        .stopB(
            BusStopSummaryDTO.builder()
                .id(p.getStopBId())
                .code(p.getStopBCode())
                .name(p.getStopBName())
                .lat(p.getStopBLat())
                .lon(p.getStopBLon())
                .build())
        .candidateLat(p.getCandidateLat())
        .candidateLon(p.getCandidateLon())
        .populationScore(p.getPopulationScore())
        .publicSpaceScore(p.getPublicSpaceScore())
        .combinedScore(p.getCombinedScore())
        .build();
  }
}
