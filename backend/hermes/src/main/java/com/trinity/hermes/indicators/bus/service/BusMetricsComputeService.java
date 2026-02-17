package com.trinity.hermes.indicators.bus.service;

import com.trinity.hermes.indicators.bus.entity.BusLiveStopTimeUpdate;
import com.trinity.hermes.indicators.bus.entity.BusLiveVehicle;
import com.trinity.hermes.indicators.bus.entity.BusRoute;
import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import com.trinity.hermes.indicators.bus.entity.BusTrip;
import com.trinity.hermes.indicators.bus.repository.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @Transactional
  public void computeMetrics() {
    log.info("Starting bus route metrics computation");

    List<BusRoute> routes = busRouteRepository.findAllOrderByShortName();
    List<BusLiveVehicle> allLiveVehicles = busLiveVehicleRepository.findLatestPositionPerVehicle();
    List<BusLiveStopTimeUpdate> recentUpdates =
        busLiveStopTimeUpdateRepository.findRecentStopTimeUpdates();

    for (BusRoute route : routes) {
      computeMetricsForRoute(route, allLiveVehicles, recentUpdates);
    }

    log.info("Completed bus route metrics computation for {} routes", routes.size());
  }

  private void computeMetricsForRoute(
      BusRoute route,
      List<BusLiveVehicle> allLiveVehicles,
      List<BusLiveStopTimeUpdate> recentUpdates) {

    List<BusTrip> routeTrips = busTripRepository.findByRouteId(route.getId());
    Set<String> routeTripIds = routeTrips.stream().map(BusTrip::getId).collect(Collectors.toSet());

    List<BusLiveVehicle> routeVehicles =
        allLiveVehicles.stream()
            .filter(v -> routeTripIds.contains(v.getTripId()))
            .collect(Collectors.toList());

    int activeVehicles = routeVehicles.size();
    int scheduledTrips = routeTrips.size();

    double utilizationPct =
        scheduledTrips > 0 ? (double) activeVehicles / scheduledTrips * 100.0 : 0.0;

    Double avgOccupancy = busRidershipRepository.findAverageOccupancyByRouteId(route.getId());
    double avgOccupancyPct = avgOccupancy != null ? avgOccupancy * 100.0 : 0.0;

    double peakOccupancyPct =
        routeVehicles.stream()
            .mapToDouble(
                v -> {
                  var ridership = busRidershipRepository.findLatestByVehicleId(v.getVehicleId());
                  if (ridership != null && ridership.getVehicleCapacity() > 0) {
                    return (double) ridership.getPassengersOnboard()
                        / ridership.getVehicleCapacity()
                        * 100.0;
                  }
                  return 0.0;
                })
            .max()
            .orElse(0.0);

    Set<Integer> routeVehicleEntryIds =
        routeVehicles.stream().map(BusLiveVehicle::getEntryId).collect(Collectors.toSet());

    List<BusLiveStopTimeUpdate> routeDelays =
        recentUpdates.stream()
            .filter(u -> routeVehicleEntryIds.contains(u.getTripUpdateEntryId()))
            .collect(Collectors.toList());

    double avgDelaySeconds =
        routeDelays.stream()
            .mapToInt(
                u ->
                    Math.max(
                        u.getArrivalDelay() != null ? u.getArrivalDelay() : 0,
                        u.getDepartureDelay() != null ? u.getDepartureDelay() : 0))
            .average()
            .orElse(0.0);

    int maxDelaySeconds =
        routeDelays.stream()
            .mapToInt(
                u ->
                    Math.max(
                        u.getArrivalDelay() != null ? u.getArrivalDelay() : 0,
                        u.getDepartureDelay() != null ? u.getDepartureDelay() : 0))
            .max()
            .orElse(0);

    long lateCount =
        routeDelays.stream()
            .filter(
                u ->
                    (u.getArrivalDelay() != null && u.getArrivalDelay() > 60)
                        || (u.getDepartureDelay() != null && u.getDepartureDelay() > 60))
            .count();
    double lateArrivalPct =
        routeDelays.isEmpty() ? 0.0 : (double) lateCount / routeDelays.size() * 100.0;

    double reliabilityPct = 100.0 - lateArrivalPct;

    String status = determineStatus(utilizationPct);

    Optional<BusRouteMetrics> existing = busRouteMetricsRepository.findByRouteId(route.getId());

    BusRouteMetrics metrics;
    if (existing.isPresent()) {
      metrics = existing.get();
    } else {
      metrics = new BusRouteMetrics();
      metrics.setRouteId(route.getId());
    }

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

    busRouteMetricsRepository.save(metrics);
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
