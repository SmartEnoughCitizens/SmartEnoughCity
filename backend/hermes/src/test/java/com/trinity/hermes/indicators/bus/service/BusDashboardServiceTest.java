package com.trinity.hermes.indicators.bus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.bus.dto.BusCommonDelayDTO;
import com.trinity.hermes.indicators.bus.dto.BusDashboardKpiDTO;
import com.trinity.hermes.indicators.bus.dto.BusLiveVehicleDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteBreakdownDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteDetailDTO;
import com.trinity.hermes.indicators.bus.dto.BusRouteUtilizationDTO;
import com.trinity.hermes.indicators.bus.dto.BusSystemPerformanceDTO;
import com.trinity.hermes.indicators.bus.entity.BusCommonDelayMV;
import com.trinity.hermes.indicators.bus.entity.BusLiveVehicle;
import com.trinity.hermes.indicators.bus.entity.BusRidership;
import com.trinity.hermes.indicators.bus.entity.BusRoute;
import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import com.trinity.hermes.indicators.bus.entity.BusStop;
import com.trinity.hermes.indicators.bus.entity.BusStopTime;
import com.trinity.hermes.indicators.bus.entity.BusTrip;
import com.trinity.hermes.indicators.bus.entity.BusTripShape;
import com.trinity.hermes.indicators.bus.repository.*;
import com.trinity.hermes.indicators.bus.repository.BusRouteBreakdownProjection;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusDashboardServiceTest {

  @Mock private BusLiveVehicleRepository busLiveVehicleRepository;
  @Mock private BusLiveStopTimeUpdateRepository busLiveStopTimeUpdateRepository;
  @Mock private BusRouteMetricsRepository busRouteMetricsRepository;
  @Mock private BusRidershipRepository busRidershipRepository;
  @Mock private BusTripRepository busTripRepository;
  @Mock private BusRouteRepository busRouteRepository;
  @Mock private BusCommonDelayMvRepository busCommonDelayMvRepository;
  @Mock private BusTripShapeRepository busTripShapeRepository;
  @Mock private BusStopTimeRepository busStopTimeRepository;
  @Mock private BusStopRepository busStopRepository;

  @InjectMocks private BusDashboardService busDashboardService;

  @Test
  void getKpis_returnsAggregatedKpis() {
    when(busLiveVehicleRepository.countActiveVehicles()).thenReturn(25L);
    when(busLiveStopTimeUpdateRepository.countActiveDelays(120)).thenReturn(3L);
    when(busRouteMetricsRepository.findFleetUtilization()).thenReturn(85.5);
    when(busRouteMetricsRepository.findAverageReliability()).thenReturn(90.0);

    BusDashboardKpiDTO kpis = busDashboardService.getKpis();

    assertThat(kpis.getTotalBusesRunning()).isEqualTo(25L);
    assertThat(kpis.getActiveDelays()).isEqualTo(3L);
    assertThat(kpis.getFleetUtilizationPct()).isEqualTo(85.5);
    assertThat(kpis.getSustainabilityScore()).isGreaterThan(0.0);
  }

  @Test
  void getKpis_withNullMetrics_returnsZeroDefaults() {
    when(busLiveVehicleRepository.countActiveVehicles()).thenReturn(0L);
    when(busLiveStopTimeUpdateRepository.countActiveDelays(120)).thenReturn(0L);
    when(busRouteMetricsRepository.findFleetUtilization()).thenReturn(null);
    when(busRouteMetricsRepository.findAverageReliability()).thenReturn(null);

    BusDashboardKpiDTO kpis = busDashboardService.getKpis();

    assertThat(kpis.getTotalBusesRunning()).isEqualTo(0L);
    assertThat(kpis.getFleetUtilizationPct()).isEqualTo(0.0);
  }

  @Test
  void getLiveVehiclePositions_returnsMappedVehicles() {
    BusLiveVehicle vehicle =
        new BusLiveVehicle(
            1,
            100,
            "trip_1",
            LocalTime.of(8, 0),
            LocalDate.now(ZoneId.of("UTC")),
            "SCHEDULED",
            0,
            53.3,
            -6.2,
            Timestamp.from(Instant.now()));

    BusTrip trip = new BusTrip("trip_1", "route_1", 1, "Sandyford", "42", 0, "shape_1");
    BusRidership ridership =
        new BusRidership(
            1, 100, "trip_1", "stop_1", 1, Timestamp.from(Instant.now()), 5, 2, 40, 80);

    com.trinity.hermes.indicators.bus.entity.BusRoute route =
        new com.trinity.hermes.indicators.bus.entity.BusRoute(
            "route_1", 1, "42", "City Center - Sandyford");

    when(busLiveVehicleRepository.findRecentVehicles()).thenReturn(List.of(vehicle));
    when(busTripRepository.findAllById(java.util.Set.of("trip_1"))).thenReturn(List.of(trip));
    when(busRouteRepository.findAllById(java.util.Set.of("route_1"))).thenReturn(List.of(route));
    when(busRidershipRepository.findLatestPerVehicle()).thenReturn(List.of(ridership));

    List<BusLiveVehicleDTO> vehicles = busDashboardService.getLiveVehiclePositions();

    assertThat(vehicles).hasSize(1);
    BusLiveVehicleDTO dto = vehicles.get(0);
    assertThat(dto.getVehicleId()).isEqualTo(100);
    assertThat(dto.getLatitude()).isEqualTo(53.3);
    assertThat(dto.getLongitude()).isEqualTo(-6.2);
    assertThat(dto.getRouteShortName()).isEqualTo("42");
    assertThat(dto.getOccupancyPct()).isEqualTo(50.0);
    assertThat(dto.getStatus()).isEqualTo("on-time");
  }

  @Test
  void getRouteUtilization_returnsAllRoutesWithStatus() {
    BusRouteMetrics metrics1 = new BusRouteMetrics();
    metrics1.setRouteId("route_1");
    metrics1.setRouteShortName("42");
    metrics1.setRouteLongName("City Center - Sandyford");
    metrics1.setUtilizationPct(95.0);
    metrics1.setActiveVehicles(10);

    BusRouteMetrics metrics2 = new BusRouteMetrics();
    metrics2.setRouteId("route_2");
    metrics2.setRouteShortName("16");
    metrics2.setRouteLongName("Dublin Airport - Ballinteer");
    metrics2.setUtilizationPct(25.0);
    metrics2.setActiveVehicles(3);

    when(busRouteMetricsRepository.findAll()).thenReturn(List.of(metrics1, metrics2));

    List<BusRouteUtilizationDTO> utilization = busDashboardService.getRouteUtilization();

    assertThat(utilization).hasSize(2);
    assertThat(utilization.get(0).getStatus()).isEqualTo("low");
    assertThat(utilization.get(1).getStatus()).isEqualTo("critical");
  }

  @Test
  void getSystemPerformance_returnsAggregatedPerformance() {
    when(busRouteMetricsRepository.findAverageReliability()).thenReturn(88.0);
    when(busRouteMetricsRepository.findAverageLateArrival()).thenReturn(12.0);

    BusSystemPerformanceDTO performance = busDashboardService.getSystemPerformance();

    assertThat(performance.getReliabilityPct()).isEqualTo(88.0);
    assertThat(performance.getLateArrivalPct()).isEqualTo(12.0);
    // assertThat(performance.getEvAdoptionPct()).isGreaterThanOrEqualTo(0.0);
  }

  @Test
  void getCommonDelays_returnsMappedResultsFromMv() {
    BusCommonDelayMV mv1 =
        new BusCommonDelayMV(1L, "today", "route_1", "42", "City Center - Sandyford", 15.5);
    BusCommonDelayMV mv2 =
        new BusCommonDelayMV(2L, "today", "route_2", "16", "Dublin Airport - Ballinteer", 8.3);

    when(busCommonDelayMvRepository.findByPeriodOrderByAvgDelayMinutesDesc("today"))
        .thenReturn(List.of(mv1, mv2));

    List<BusCommonDelayDTO> delays = busDashboardService.getCommonDelays("today");

    assertThat(delays).hasSize(2);
    assertThat(delays.get(0).getRouteShortName()).isEqualTo("42");
    assertThat(delays.get(0).getAvgDelayMinutes()).isEqualTo(15.5);
    assertThat(delays.get(1).getRouteShortName()).isEqualTo("16");
  }

  @Test
  void getCommonDelays_withInvalidFilter_defaultsToToday() {
    when(busCommonDelayMvRepository.findByPeriodOrderByAvgDelayMinutesDesc("today"))
        .thenReturn(List.of());

    List<BusCommonDelayDTO> delays = busDashboardService.getCommonDelays("invalid");

    assertThat(delays).isEmpty();
    org.mockito.Mockito.verify(busCommonDelayMvRepository)
        .findByPeriodOrderByAvgDelayMinutesDesc("today");
  }

  @Test
  void getRouteBreakdown_returnsMappedPerStopData() {
    BusRouteBreakdownProjection p =
        new BusRouteBreakdownProjection() {
          @Override
          public String getStopId() {
            return "8540B1559201";
          }

          @Override
          public Double getAvgDelayMinutes() {
            return 5.2;
          }

          @Override
          public Double getMaxDelayMinutes() {
            return 12.0;
          }

          @Override
          public Long getTripCount() {
            return 25L;
          }
        };

    when(busLiveStopTimeUpdateRepository.findBreakdownByRoute("route_1", "today"))
        .thenReturn(List.of(p));

    List<BusRouteBreakdownDTO> breakdown =
        busDashboardService.getRouteBreakdown("route_1", "today");

    assertThat(breakdown).hasSize(1);
    assertThat(breakdown.get(0).getStopId()).isEqualTo("8540B1559201");
    assertThat(breakdown.get(0).getAvgDelayMinutes()).isEqualTo(5.2);
    assertThat(breakdown.get(0).getTripCount()).isEqualTo(25L);
  }

  @Test
  void getRouteDetail_returnsRouteMetadataAndShapeFromFirstTrip() {
    BusRoute route = new BusRoute("r1", 1, "42", "A to B");
    BusTrip trip = new BusTrip("trip_a", "r1", 1, "heads", "short", 0, "shape_1");
    List<BusTripShape> shapes =
        List.of(
            new BusTripShape(1, "shape_1", 0, 53.0, -6.2, 0.0),
            new BusTripShape(2, "shape_1", 1, 53.1, -6.3, 100.0));

    when(busRouteRepository.findById("r1")).thenReturn(Optional.of(route));
    when(busTripRepository.findFirstByRouteIdOrderByIdAsc("r1")).thenReturn(Optional.of(trip));
    when(busTripShapeRepository.findByShapeIdOrderByPtSequenceAsc("shape_1")).thenReturn(shapes);

    BusStopTime st1 =
        new BusStopTime(
            1, "trip_a", "stop_1", Time.valueOf("08:00:00"), Time.valueOf("08:01:00"), 0, null);
    BusStop stop1 = new BusStop("stop_1", 100, "First Stop", null, 53.0, -6.2);
    when(busStopTimeRepository.findByTripIdOrderBySequenceAsc("trip_a")).thenReturn(List.of(st1));
    when(busStopRepository.findAllById(any())).thenReturn(List.of(stop1));

    BusRouteDetailDTO dto = busDashboardService.getRouteDetail("r1");

    assertThat(dto.getRouteId()).isEqualTo("r1");
    assertThat(dto.getShortName()).isEqualTo("42");
    assertThat(dto.getRepresentativeTripId()).isEqualTo("trip_a");
    assertThat(dto.getShapeId()).isEqualTo("shape_1");
    assertThat(dto.getShape()).hasSize(2);
    assertThat(dto.getShape().get(0).getSequence()).isEqualTo(0);
    assertThat(dto.getShape().get(1).getLat()).isEqualTo(53.1);
    assertThat(dto.getStops()).hasSize(1);
    assertThat(dto.getStops().get(0).getStopId()).isEqualTo("stop_1");
    assertThat(dto.getStops().get(0).getName()).isEqualTo("First Stop");
  }
}
