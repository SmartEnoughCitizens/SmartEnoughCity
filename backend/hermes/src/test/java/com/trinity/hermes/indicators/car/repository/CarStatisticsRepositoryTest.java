package com.trinity.hermes.indicators.car.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
class CarStatisticsRepositoryTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18-alpine").withInitScript("init-test-schemas.sql");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private CarStatisticsRepository carStatisticsRepository;
  @Autowired private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager
        .getEntityManager()
        .createNativeQuery("DELETE FROM external_data.vehicle_yearly")
        .executeUpdate();
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "INSERT INTO external_data.vehicle_yearly (year, taxation_class, fuel_type, count)"
                + " VALUES (2020, 'PRIVATE', 'PETROL',   3000),"
                + "        (2021, 'PRIVATE', 'PETROL',   2000),"
                + "        (2020, 'PRIVATE', 'DIESEL',   1500),"
                + "        (2020, 'PRIVATE', 'ELECTRIC',  500),"
                + "        (2014, 'PRIVATE', 'ELECTRIC',  100)")
        .executeUpdate();
  }

  @Test
  void findTotalCountByFuelType_returnsSummedCountsPerFuelType() {
    List<Object[]> rows = carStatisticsRepository.findTotalCountByFuelType();

    assertThat(rows).hasSize(3);

    // PETROL: 3000 + 2000 = 5000
    Object[] petrolRow =
        rows.stream().filter(r -> "PETROL".equals(r[0])).findFirst().orElseThrow();
    assertThat(((Number) petrolRow[1]).longValue()).isEqualTo(5000L);

    // DIESEL: 1500
    Object[] dieselRow =
        rows.stream().filter(r -> "DIESEL".equals(r[0])).findFirst().orElseThrow();
    assertThat(((Number) dieselRow[1]).longValue()).isEqualTo(1500L);

    // ELECTRIC: 500 + 100 = 600 (all years summed)
    Object[] electricRow =
        rows.stream().filter(r -> "ELECTRIC".equals(r[0])).findFirst().orElseThrow();
    assertThat(((Number) electricRow[1]).longValue()).isEqualTo(600L);
  }

  @Test
  void findElectricVehicleCountFrom2015_excludesYearsBefore2015() {
    Long count = carStatisticsRepository.findElectricVehicleCountFrom2015();

    // year 2020 ELECTRIC = 500; year 2014 ELECTRIC (100) is excluded by WHERE year >= 2015
    assertThat(count).isEqualTo(500L);
  }

  @Test
  void findElectricVehicleCountFrom2015_withNoElectricRows_returnsNull() {
    entityManager
        .getEntityManager()
        .createNativeQuery("DELETE FROM external_data.vehicle_yearly WHERE fuel_type = 'ELECTRIC'")
        .executeUpdate();

    Long count = carStatisticsRepository.findElectricVehicleCountFrom2015();

    // SUM() over an empty result set returns NULL in SQL
    assertThat(count).isNull();
  }

  @Test
  void findElectricVehicleCountFrom2015_withOnlyPre2015Electric_returnsNull() {
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "DELETE FROM external_data.vehicle_yearly WHERE fuel_type = 'ELECTRIC' AND year >= 2015")
        .executeUpdate();

    Long count = carStatisticsRepository.findElectricVehicleCountFrom2015();

    // Only the 2014 row remains, which is filtered out by year >= 2015 → SUM is NULL
    assertThat(count).isNull();
  }

  @Test
  void findTotalCountByFuelType_withSingleRow_returnsSingleEntry() {
    entityManager
        .getEntityManager()
        .createNativeQuery("DELETE FROM external_data.vehicle_yearly")
        .executeUpdate();
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "INSERT INTO external_data.vehicle_yearly (year, taxation_class, fuel_type, count)"
                + " VALUES (2022, 'PRIVATE', 'HYBRID', 750)")
        .executeUpdate();

    List<Object[]> rows = carStatisticsRepository.findTotalCountByFuelType();

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)[0]).isEqualTo("HYBRID");
    assertThat(((Number) rows.get(0)[1]).longValue()).isEqualTo(750L);
  }
}
