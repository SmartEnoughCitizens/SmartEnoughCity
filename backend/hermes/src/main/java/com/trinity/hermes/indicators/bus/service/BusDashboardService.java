package com.trinity.hermes.indicators.bus.service;

import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
import com.trinity.hermes.indicators.bus.dto.BusSystemPerformanceDTO;
import com.trinity.hermes.indicators.bus.entity.BusLiveVehicle;
import com.trinity.hermes.indicators.bus.entity.BusRidership;
import com.trinity.hermes.indicators.bus.entity.BusRoute;
import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import com.trinity.hermes.indicators.bus.entity.BusTrip;
import com.trinity.hermes.indicators.bus.repository.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final BusRouteRepository busRouteRepository;

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

    List<BusLiveVehicle> vehicles = busLiveVehicleRepository.findLatestPositionPerVehicle();
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
        busRidershipRepository.findLatestPerVehicle().stream()
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
}
