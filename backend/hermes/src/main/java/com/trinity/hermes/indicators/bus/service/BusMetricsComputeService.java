package com.trinity.hermes.indicators.bus.service;

import com.trinity.hermes.indicators.bus.entity.BusLiveStopTimeUpdate;
import com.trinity.hermes.indicators.bus.entity.BusLiveVehicle;
import com.trinity.hermes.indicators.bus.entity.BusRidership;
import com.trinity.hermes.indicators.bus.entity.BusRoute;
import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import com.trinity.hermes.indicators.bus.entity.BusTrip;
import com.trinity.hermes.indicators.bus.repository.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusMetricsComputeService {

  private final BusRouteRepository busRouteRepository;
  private final BusTripRepository busTripRepository;
  private final BusLiveVehicleRepository busLiveVehicleRepository;
  private final BusRidershipRepository busRidershipRepository;
  private final BusLiveStopTimeUpdateRepository busLiveStopTimeUpdateRepository;
  private final BusRouteMetricsRepository busRouteMetricsRepository;
  private final BusStopTimeRepository busStopTimeRepository;

  @Scheduled(fixedRate = 300000, initialDelay = 5000)
  @Transactional
  public void computeMetrics() {
    try {
      log.info("Starting bus route metrics computation");

      busRouteMetricsRepository.deleteAll();

      List<BusRoute> routes = busRouteRepository.findAllOrderByShortName();
      List<BusLiveVehicle> recentVehicles =
          busLiveVehicleRepository.findLatestPositionPerVehicle();
      List<Object[]> delayRows =
          busLiveStopTimeUpdateRepository.findDelaysByVehicle();

      // All trips grouped by route_id — used for scheduled trip count
      List<BusTrip> allTrips = busTripRepository.findAll();
      Map<String, Set<String>> allTripIdsByRouteId =
          allTrips.stream().collect(
              Collectors.groupingBy(
                  BusTrip::getRouteId,
                  Collectors.mapping(BusTrip::getId, Collectors.toSet())));

      // Find ALL trips whose stop_time window covers the current Dublin time
      // (no IN clause — no service_id filtering — purely time-based)
      java.time.LocalTime nowTime =
          java.time.LocalTime.now(java.time.ZoneId.of("Europe/Dublin"));
      java.sql.Time sqlNow = java.sql.Time.valueOf(nowTime);
      Set<String> currentlyActiveTrips =
          new java.util.HashSet<>(busStopTimeRepository.findAllActiveTripsAtTime(sqlNow));
      // Index vehicles by trip_id (same source as the map) — used for utilization numerator
      Map<String, List<BusLiveVehicle>> recentVehiclesByTripId =
          recentVehicles.stream().collect(Collectors.groupingBy(BusLiveVehicle::getTripId));

      // Index delays by vehicle_id: vehicle_id → list of [arrivalDelay, departureDelay]
      Map<Integer, List<int[]>> delaysByVehicleId = new java.util.HashMap<>();
      for (Object[] row : delayRows) {
        int vehicleId = ((Number) row[0]).intValue();
        int arrivalDelay = row[1] != null ? ((Number) row[1]).intValue() : 0;
        int departureDelay = row[2] != null ? ((Number) row[2]).intValue() : 0;
        delaysByVehicleId.computeIfAbsent(vehicleId, k -> new java.util.ArrayList<>())
            .add(new int[]{arrivalDelay, departureDelay});
      }

      // Bulk-load latest ridership per vehicle
      List<BusRidership> allRidership = busRidershipRepository.findAll();
      Map<Integer, BusRidership> latestRidershipByVehicleId =
          allRidership.stream()
              .collect(
                  Collectors.toMap(
                      BusRidership::getVehicleId,
                      r -> r,
                      (r1, r2) ->
                          r1.getTimestamp().after(r2.getTimestamp()) ? r1 : r2));

      List<BusRouteMetrics> toSave = new ArrayList<>();

      for (BusRoute route : routes) {
        BusRouteMetrics metrics =
            computeMetricsForRoute(
                route,
                allTripIdsByRouteId,
                recentVehiclesByTripId,
                delaysByVehicleId,
                latestRidershipByVehicleId,
                currentlyActiveTrips);
        toSave.add(metrics);
      }

      busRouteMetricsRepository.saveAll(toSave);

      log.info("Completed bus route metrics computation for {} routes", routes.size());
    } catch (Exception e) {
      log.error("Failed to compute bus route metrics", e);
    }
  }

  private BusRouteMetrics computeMetricsForRoute(
      BusRoute route,
      Map<String, Set<String>> allTripIdsByRouteId,
      Map<String, List<BusLiveVehicle>> recentVehiclesByTripId,
      Map<Integer, List<int[]>> delaysByVehicleId,
      Map<Integer, BusRidership> latestRidershipByVehicleId,
      Set<String> currentlyActiveTrips) {

    // All trip IDs for this route (across all services)
    Set<String> allRouteTripIds = allTripIdsByRouteId.getOrDefault(route.getId(), Set.of());

    // Scheduled buses = trips whose stop_time window covers right now
    int scheduledTrips = (int) allRouteTripIds.stream()
        .filter(currentlyActiveTrips::contains)
        .count();

    // Active buses = recent live vehicles reporting for this route
    Set<String> vehicleActiveIds =
        recentVehiclesByTripId.keySet().stream()
            .filter(allRouteTripIds::contains)
            .collect(Collectors.toSet());
    int activeVehicles = vehicleActiveIds.size();

    // Utilization = (active buses / scheduled buses) × 100, capped at 100%
    double utilizationPct =
        scheduledTrips > 0 ? Math.min((double) activeVehicles / scheduledTrips * 100.0, 100.0) : 0.0;

    // Use recent vehicles for occupancy/delay calculation
    List<BusLiveVehicle> routeVehicles =
        vehicleActiveIds.stream()
            .flatMap(tripId -> recentVehiclesByTripId.getOrDefault(tripId, List.of()).stream())
            .toList();

    // Compute occupancy from pre-loaded ridership
    double avgOccupancyPct = 0.0;
    double peakOccupancyPct = 0.0;
    int occupancyCount = 0;
    double occupancySum = 0.0;

    for (BusLiveVehicle vehicle : routeVehicles) {
      BusRidership ridership = latestRidershipByVehicleId.get(vehicle.getVehicleId());
      if (ridership != null && ridership.getVehicleCapacity() > 0) {
        double pct =
            (double) ridership.getPassengersOnboard() / ridership.getVehicleCapacity() * 100.0;
        occupancySum += pct;
        occupancyCount++;
        if (pct > peakOccupancyPct) {
          peakOccupancyPct = pct;
        }
      }
    }
    if (occupancyCount > 0) {
      avgOccupancyPct = occupancySum / occupancyCount;
    }

    // Collect delays for this route's vehicles via vehicle_id (correct linkage)
    Set<Integer> routeVehicleIds =
        routeVehicles.stream().map(BusLiveVehicle::getVehicleId).collect(Collectors.toSet());

    List<int[]> routeDelays =
        routeVehicleIds.stream()
            .flatMap(vid -> delaysByVehicleId.getOrDefault(vid, List.of()).stream())
            .toList();

    double avgDelaySeconds =
        routeDelays.stream()
            .mapToInt(d -> Math.max(d[0], d[1]))
            .average()
            .orElse(0.0);

    int maxDelaySeconds =
        routeDelays.stream()
            .mapToInt(d -> Math.max(d[0], d[1]))
            .max()
            .orElse(0);

    long lateCount =
        routeDelays.stream()
            .filter(d -> d[0] > 60 || d[1] > 60)
            .count();
    double lateArrivalPct =
        routeDelays.isEmpty() ? 0.0 : (double) lateCount / routeDelays.size() * 100.0;

    double reliabilityPct = 100.0 - lateArrivalPct;

    String status = determineStatus(utilizationPct);

    BusRouteMetrics metrics = new BusRouteMetrics();
    metrics.setRouteId(route.getId());

    metrics.setRouteShortName(route.getShortName());
    metrics.setRouteLongName(route.getLongName());
    metrics.setActiveVehicles(activeVehicles);
    metrics.setScheduledTrips(scheduledTrips);
    metrics.setUtilizationPct(utilizationPct);
    metrics.setAvgDelaySeconds(avgDelaySeconds);
    metrics.setMaxDelaySeconds(maxDelaySeconds);
    metrics.setAvgOccupancyPct(avgOccupancyPct);
    metrics.setPeakOccupancyPct(peakOccupancyPct);
    metrics.setReliabilityPct(reliabilityPct);
    metrics.setLateArrivalPct(lateArrivalPct);
    metrics.setStatus(status);
    metrics.setComputedAt(Timestamp.from(Instant.now()));

    return metrics;
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
}