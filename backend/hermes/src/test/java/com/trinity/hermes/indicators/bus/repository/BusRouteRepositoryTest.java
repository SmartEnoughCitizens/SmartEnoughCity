package com.trinity.hermes.indicators.bus.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trinity.hermes.indicators.bus.entity.BusRoute;
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
class BusRouteRepositoryTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18-alpine")
          .withInitScript("init-test-schemas.sql");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private BusRouteRepository busRouteRepository;
  @Autowired private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {
    busRouteRepository.deleteAll();
    entityManager
        .getEntityManager()
        .createNativeQuery(
            "INSERT INTO external_data.bus_routes (id, agency_id, short_name, long_name)"
                + " VALUES ('route_1', 1, '42', 'City Center - Sandyford'),"
                + " ('route_2', 1, '16', 'Dublin Airport - Ballinteer'),"
                + " ('route_3', 2, 'G1', 'Howth - Belfield')")
        .executeUpdate();
  }

  @Test
  void findAllOrderByShortName_returnsRoutesSortedByShortName() {
    List<BusRoute> routes = busRouteRepository.findAllOrderByShortName();

    assertThat(routes).hasSize(3);
    assertThat(routes.get(0).getShortName()).isEqualTo("16");
    assertThat(routes.get(1).getShortName()).isEqualTo("42");
    assertThat(routes.get(2).getShortName()).isEqualTo("G1");
  }

  @Test
  void findByAgencyId_returnsOnlyMatchingRoutes() {
    List<BusRoute> agency1Routes = busRouteRepository.findByAgencyId(1);
    List<BusRoute> agency2Routes = busRouteRepository.findByAgencyId(2);

    assertThat(agency1Routes).hasSize(2);
    assertThat(agency2Routes).hasSize(1);
    assertThat(agency2Routes.get(0).getShortName()).isEqualTo("G1");
  }

  @Test
  void findById_returnsRoute() {
    BusRoute route = busRouteRepository.findById("route_3").orElse(null);

    assertThat(route).isNotNull();
    assertThat(route.getShortName()).isEqualTo("G1");
    assertThat(route.getLongName()).isEqualTo("Howth - Belfield");
  }
}
