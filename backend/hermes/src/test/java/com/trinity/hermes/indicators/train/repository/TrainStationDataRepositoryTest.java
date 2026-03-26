package com.trinity.hermes.indicators.train.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TrainStationDataRepositoryTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18-alpine").withInitScript("init-test-schemas.sql");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private TrainStationDataRepository repository;
  @Autowired private TestEntityManager entityManager;

  /** Auto-incrementing ID counter so every test's inserts have unique PKs. */
  private final AtomicInteger idSeq = new AtomicInteger(1);

  @BeforeEach
  void setUp() {
    entityManager
        .getEntityManager()
        .createNativeQuery("DELETE FROM external_data.irish_rail_station_data")
        .executeUpdate();
    idSeq.set(1);
  }

  // ── Test 1 ────────────────────────────────────────────────────────

  @Test
  void findAverageLateMinutes_returnsAverageOfNonNullRecords() {
    insertStationData("DART", "E801", "Connolly", "Malahide", 10, "STOP", "Northbound");
    insertStationData("DART", "E801", "Pearse", "Malahide", 20, "STOP", "Northbound");
    insertStationDataWithNullDelay("DART", "E802", "Connolly", "Malahide", "STOP", "Northbound");

    Double avg = repository.findAverageLateMinutes();

    // Only the two non-null records (10 + 20) / 2 = 15.0
    assertThat(avg).isEqualTo(15.0);
  }

  // ── Test 2 ────────────────────────────────────────────────────────

  @Test
  void findLateArrivalPct_returnsCorrectPercentage() {
    // 2 late (late_minutes > 0), 2 on-time (late_minutes = 0)
    insertStationData("DART", "E801", "Connolly", "Malahide", 5, "STOP", "Northbound");
    insertStationData("DART", "E801", "Pearse", "Malahide", 8, "STOP", "Northbound");
    insertStationData("DART", "E802", "Connolly", "Malahide", 0, "STOP", "Northbound");
    insertStationData("DART", "E802", "Pearse", "Malahide", 0, "STOP", "Northbound");

    Double pct = repository.findLateArrivalPct();

    assertThat(pct).isEqualTo(50.0);
  }

  // ── Test 3 ────────────────────────────────────────────────────────

  @Test
  void findAverageDueInMinutes_returnsCorrectAverage() {
    insertStationDataWithDue("DART", "E801", "Connolly", "Malahide", 6, "STOP", "Northbound");
    insertStationDataWithDue("DART", "E802", "Connolly", "Malahide", 14, "STOP", "Southbound");

    Double avg = repository.findAverageDueInMinutes();

    assertThat(avg).isEqualTo(10.0);
  }

  // ── Test 4 ────────────────────────────────────────────────────────

  @Test
  void findFrequentlyDelayedTrains_excludesOriginStops() {
    // ORIGIN stop — must NOT be included in the delay calculation
    insertStationData("DART", "E801", "Greystones", "Malahide", 30, "ORIGIN", "Northbound");
    // STOP — must be included
    insertStationData("DART", "E801", "Bray", "Malahide", 10, "STOP", "Northbound");

    List<TrainDelayProjection> results = repository.findFrequentlyDelayedTrains();

    assertThat(results).hasSize(1);
    // Total avg delay should only reflect the STOP record (10 min), not the ORIGIN (30 min)
    assertThat(results.get(0).getTotalAvgDelayMinutes()).isEqualTo(10.0);
  }

  // ── Test 5 ────────────────────────────────────────────────────────

  @Test
  void findFrequentlyDelayedTrains_orderedByTotalDelayDescending() {
    // E801 — low delay
    insertStationData("DART", "E801", "Greystones", "Malahide", 2, "STOP", "Northbound");
    // E802 — high delay
    insertStationData("DART", "E802", "Greystones", "Bray", 25, "STOP", "Southbound");

    List<TrainDelayProjection> results = repository.findFrequentlyDelayedTrains();

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results.get(0).getTrainCode()).isEqualTo("E802");
    assertThat(results.get(1).getTrainCode()).isEqualTo("E801");
  }

  // ── Test 6 ────────────────────────────────────────────────────────

  @Test
  void findFrequentlyDelayedTrains_nullDirectionBecomesUnknown() {
    insertStationDataNullDirection("DART", "E801", "Connolly", "Malahide", 5, "STOP");

    List<TrainDelayProjection> results = repository.findFrequentlyDelayedTrains();

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getDirection()).isEqualTo("Unknown");
  }

  // ── Test 7 ────────────────────────────────────────────────────────

  @Test
  void findFrequentlyDelayedTrains_withNoData_returnsEmptyList() {
    List<TrainDelayProjection> results = repository.findFrequentlyDelayedTrains();

    assertThat(results).isNotNull();
    assertThat(results).isEmpty();
  }

  // ── Insert helpers ────────────────────────────────────────────────

  private void insertStationData(
      String stationCode,
      String trainCode,
      String origin,
      String destination,
      int lateMinutes,
      String locationType,
      String direction) {
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "INSERT INTO external_data.irish_rail_station_data "
                + "(id, station_code, train_code, train_date, origin, destination, "
                + " late_minutes, location_type, direction, fetched_at) "
                + "VALUES (:id, :stationCode, :trainCode, CURRENT_DATE, :origin, :destination, "
                + " :lateMinutes, :locationType, :direction, NOW())")
        .setParameter("id", idSeq.getAndIncrement())
        .setParameter("stationCode", stationCode)
        .setParameter("trainCode", trainCode)
        .setParameter("origin", origin)
        .setParameter("destination", destination)
        .setParameter("lateMinutes", lateMinutes)
        .setParameter("locationType", locationType)
        .setParameter("direction", direction)
        .executeUpdate();
  }

  private void insertStationDataWithNullDelay(
      String stationCode,
      String trainCode,
      String origin,
      String destination,
      String locationType,
      String direction) {
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "INSERT INTO external_data.irish_rail_station_data "
                + "(id, station_code, train_code, train_date, origin, destination, "
                + " late_minutes, location_type, direction, fetched_at) "
                + "VALUES (:id, :stationCode, :trainCode, CURRENT_DATE, :origin, :destination, "
                + " NULL, :locationType, :direction, NOW())")
        .setParameter("id", idSeq.getAndIncrement())
        .setParameter("stationCode", stationCode)
        .setParameter("trainCode", trainCode)
        .setParameter("origin", origin)
        .setParameter("destination", destination)
        .setParameter("locationType", locationType)
        .setParameter("direction", direction)
        .executeUpdate();
  }

  private void insertStationDataWithDue(
      String stationCode,
      String trainCode,
      String origin,
      String destination,
      int dueInMinutes,
      String locationType,
      String direction) {
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "INSERT INTO external_data.irish_rail_station_data "
                + "(id, station_code, train_code, train_date, origin, destination, "
                + " due_in_minutes, late_minutes, location_type, direction, fetched_at) "
                + "VALUES (:id, :stationCode, :trainCode, CURRENT_DATE, :origin, :destination, "
                + " :dueInMinutes, 0, :locationType, :direction, NOW())")
        .setParameter("id", idSeq.getAndIncrement())
        .setParameter("stationCode", stationCode)
        .setParameter("trainCode", trainCode)
        .setParameter("origin", origin)
        .setParameter("destination", destination)
        .setParameter("dueInMinutes", dueInMinutes)
        .setParameter("locationType", locationType)
        .setParameter("direction", direction)
        .executeUpdate();
  }

  private void insertStationDataNullDirection(
      String stationCode,
      String trainCode,
      String origin,
      String destination,
      int lateMinutes,
      String locationType) {
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "INSERT INTO external_data.irish_rail_station_data "
                + "(id, station_code, train_code, train_date, origin, destination, "
                + " late_minutes, location_type, direction, fetched_at) "
                + "VALUES (:id, :stationCode, :trainCode, CURRENT_DATE, :origin, :destination, "
                + " :lateMinutes, :locationType, NULL, NOW())")
        .setParameter("id", idSeq.getAndIncrement())
        .setParameter("stationCode", stationCode)
        .setParameter("trainCode", trainCode)
        .setParameter("origin", origin)
        .setParameter("destination", destination)
        .setParameter("lateMinutes", lateMinutes)
        .setParameter("locationType", locationType)
        .executeUpdate();
  }
}
