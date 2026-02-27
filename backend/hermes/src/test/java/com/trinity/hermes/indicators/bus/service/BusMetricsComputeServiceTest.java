package com.trinity.hermes.indicators.bus.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import com.trinity.hermes.indicators.bus.repository.BusRouteMetricsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class BusMetricsComputeServiceTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18-alpine").withInitScript("init-test-schemas.sql");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @MockitoBean JwtDecoder jwtDecoder;

  @Autowired private BusMetricsComputeService busMetricsComputeService;
  @Autowired private BusRouteMetricsRepository busRouteMetricsRepository;
  @Autowired private jakarta.persistence.EntityManager entityManager;

  @BeforeEach
  void setUp() {
    busRouteMetricsRepository.deleteAll();
    entityManager.createNativeQuery("DELETE FROM external_data.bus_ridership").executeUpdate();
    entityManager
        .createNativeQuery("DELETE FROM external_data.bus_live_trip_updates_stop_time_updates")
        .executeUpdate();
    entityManager
        .createNativeQuery("DELETE FROM external_data.bus_live_trip_updates")
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM external_data.bus_live_vehicles").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM external_data.bus_trips").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM external_data.bus_routes").executeUpdate();
  }

  @Test
  void computeMetrics_withActiveVehiclesAndRidership_createsMetrics() {
    insertRoute("route_1", 1, "42", "City Center - Sandyford");
    insertTrip("trip_1", "route_1", 1, "Sandyford", "42", 0, "shape_1");
    insertTrip("trip_2", "route_1", 1, "Sandyford", "42", 0, "shape_1");
    insertLiveVehicle(1, 100, "trip_1", "SCHEDULED", 0, 53.3, -6.2);
    insertLiveVehicle(2, 101, "trip_2", "SCHEDULED", 0, 53.31, -6.21);
    insertRidership(1, 100, "trip_1", "stop_1", 1, 40, 80);
    insertRidership(2, 101, "trip_1", "stop_1", 1, 60, 80);
    insertTripUpdate(1, "trip_1", 100);
    insertStopTimeUpdate(1, 1, "stop_1", 1, "SCHEDULED", 120, 90);

    busMetricsComputeService.computeMetrics();

    Optional<BusRouteMetrics> metrics = busRouteMetricsRepository.findByRouteId("route_1");
    assertThat(metrics).isPresent();
    assertThat(metrics.get().getRouteShortName()).isEqualTo("42");
    assertThat(metrics.get().getActiveVehicles()).isEqualTo(2);
    assertThat(metrics.get().getAvgOccupancyPct()).isGreaterThan(0.0);
    assertThat(metrics.get().getAvgDelaySeconds()).isGreaterThan(0.0);
    assertThat(metrics.get().getComputedAt()).isNotNull();
  }

  @Test
  void computeMetrics_withNoLiveVehicles_createsMetricsWithZeroVehicles() {
    insertRoute("route_2", 1, "16", "Dublin Airport - Ballinteer");

    busMetricsComputeService.computeMetrics();

    Optional<BusRouteMetrics> metrics = busRouteMetricsRepository.findByRouteId("route_2");
    assertThat(metrics).isPresent();
    assertThat(metrics.get().getActiveVehicles()).isEqualTo(0);
    assertThat(metrics.get().getUtilizationPct()).isEqualTo(0.0);
  }

  @Test
  void computeMetrics_calledTwice_updatesExistingMetrics() {
    insertRoute("route_1", 1, "42", "City Center - Sandyford");
    insertTrip("trip_1", "route_1", 1, "Sandyford", "42", 0, "shape_1");
    insertLiveVehicle(1, 100, "trip_1", "SCHEDULED", 0, 53.3, -6.2);

    busMetricsComputeService.computeMetrics();
    busMetricsComputeService.computeMetrics();

    long count =
        busRouteMetricsRepository.findAll().stream()
            .filter(m -> "route_1".equals(m.getRouteId()))
            .count();
    assertThat(count).isEqualTo(1);
  }

  private void insertRoute(String id, int agencyId, String shortName, String longName) {
    entityManager
        .createNativeQuery(
            "INSERT INTO external_data.bus_routes (id, agency_id, short_name, long_name)"
                + " VALUES (:id, :agencyId, :shortName, :longName)")
        .setParameter("id", id)
        .setParameter("agencyId", agencyId)
        .setParameter("shortName", shortName)
        .setParameter("longName", longName)
        .executeUpdate();
  }

  private void insertTrip(
      String id,
      String routeId,
      int serviceId,
      String headsign,
      String shortName,
      int directionId,
      String shapeId) {
    entityManager
        .createNativeQuery(
            "INSERT INTO external_data.bus_trips"
                + " (id, route_id, service_id, headsign, short_name, direction_id, shape_id)"
                + " VALUES (:id, :routeId, :serviceId, :headsign, :shortName, :directionId,"
                + " :shapeId)")
        .setParameter("id", id)
        .setParameter("routeId", routeId)
        .setParameter("serviceId", serviceId)
        .setParameter("headsign", headsign)
        .setParameter("shortName", shortName)
        .setParameter("directionId", directionId)
        .setParameter("shapeId", shapeId)
        .executeUpdate();
  }

  private void insertLiveVehicle(
      int entryId,
      int vehicleId,
      String tripId,
      String scheduleRelationship,
      int directionId,
      double lat,
      double lon) {
    entityManager
        .createNativeQuery(
            "INSERT INTO external_data.bus_live_vehicles"
                + " (entry_id, vehicle_id, trip_id, start_time, start_date,"
                + " schedule_relationship, direction_id, lat, lon, timestamp)"
                + " VALUES (:entryId, :vehicleId, :tripId, '08:00:00', CURRENT_DATE,"
                + " :scheduleRelationship, :directionId, :lat, :lon, NOW())")
        .setParameter("entryId", entryId)
        .setParameter("vehicleId", vehicleId)
        .setParameter("tripId", tripId)
        .setParameter("scheduleRelationship", scheduleRelationship)
        .setParameter("directionId", directionId)
        .setParameter("lat", lat)
        .setParameter("lon", lon)
        .executeUpdate();
  }

  private void insertRidership(
      int entryId,
      int vehicleId,
      String tripId,
      String nearestStopId,
      int stopSequence,
      int passengersOnboard,
      int vehicleCapacity) {
    entityManager
        .createNativeQuery(
            "INSERT INTO external_data.bus_ridership"
                + " (entry_id, vehicle_id, trip_id, nearest_stop_id, stop_sequence,"
                + " timestamp, passengers_boarding, passengers_alighting,"
                + " passengers_onboard, vehicle_capacity)"
                + " VALUES (:entryId, :vehicleId, :tripId, :nearestStopId, :stopSequence,"
                + " NOW(), 5, 2, :passengersOnboard, :vehicleCapacity)")
        .setParameter("entryId", entryId)
        .setParameter("vehicleId", vehicleId)
        .setParameter("tripId", tripId)
        .setParameter("nearestStopId", nearestStopId)
        .setParameter("stopSequence", stopSequence)
        .setParameter("passengersOnboard", passengersOnboard)
        .setParameter("vehicleCapacity", vehicleCapacity)
        .executeUpdate();
  }

  private void insertTripUpdate(int entryId, String tripId, int vehicleId) {
    entityManager
        .createNativeQuery(
            "INSERT INTO external_data.bus_live_trip_updates"
                + " (entry_id, trip_id, start_time, start_date, schedule_relationship,"
                + " direction_id, vehicle_id, timestamp)"
                + " VALUES (:entryId, :tripId, '08:00:00', CURRENT_DATE, 'SCHEDULED',"
                + " 0, :vehicleId, NOW())")
        .setParameter("entryId", entryId)
        .setParameter("tripId", tripId)
        .setParameter("vehicleId", vehicleId)
        .executeUpdate();
  }

  private void insertStopTimeUpdate(
      int entryId,
      int tripUpdateEntryId,
      String stopId,
      int stopSequence,
      String scheduleRelationship,
      int arrivalDelay,
      int departureDelay) {
    entityManager
        .createNativeQuery(
            "INSERT INTO external_data.bus_live_trip_updates_stop_time_updates"
                + " (entry_id, trip_update_entry_id, stop_id, stop_sequence,"
                + " schedule_relationship, arrival_delay, departure_delay)"
                + " VALUES (:entryId, :tripUpdateEntryId, :stopId, :stopSequence,"
                + " :scheduleRelationship, :arrivalDelay, :departureDelay)")
        .setParameter("entryId", entryId)
        .setParameter("tripUpdateEntryId", tripUpdateEntryId)
        .setParameter("stopId", stopId)
        .setParameter("stopSequence", stopSequence)
        .setParameter("scheduleRelationship", scheduleRelationship)
        .setParameter("arrivalDelay", arrivalDelay)
        .setParameter("departureDelay", departureDelay)
        .executeUpdate();
  }
}
