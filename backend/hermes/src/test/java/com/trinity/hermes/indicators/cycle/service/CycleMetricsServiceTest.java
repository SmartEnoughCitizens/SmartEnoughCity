package com.trinity.hermes.indicators.cycle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinity.hermes.indicators.cycle.dto.NetworkKpiDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.dto.StationTimeSeriesDTO;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesHistoryRepository;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesSnapshotRepository;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesStationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CycleMetricsServiceTest {

  @Mock DublinBikesStationRepository stationRepository;
  @Mock DublinBikesSnapshotRepository snapshotRepository;
  @Mock DublinBikesHistoryRepository historyRepository;

  @InjectMocks CycleMetricsService service;

  // -------------------------------------------------------------------------
  // Helper: avoids List.of(Object[]...) varargs ambiguity
  // -------------------------------------------------------------------------

  /** Safely creates a List<Object[]> from one or more Object[] rows. */
  @SafeVarargs
  private static List<Object[]> rows(Object[]... items) {
    List<Object[]> list = new ArrayList<>();
    for (Object[] item : items) {
      list.add(item);
    }
    return list;
  }

  // -------------------------------------------------------------------------
  // Helper row builders (mirroring native query column order)
  // -------------------------------------------------------------------------

  /**
   * Builds a snapshot row matching findLatestSnapshotPerStation column order: 0:station_id, 1:name,
   * 2:short_name, 3:address, 4:latitude, 5:longitude, 6:capacity, 7:region_id, 8:available_bikes,
   * 9:available_docks, 10:disabled_bikes, 11:disabled_docks, 12:is_installed, 13:is_renting,
   * 14:is_returning, 15:last_reported, 16:snapshot_timestamp
   */
  private Object[] buildSnapshotRow(
      int stationId, int availableBikes, int availableDocks, int capacity) {
    Timestamp now = Timestamp.from(Instant.now());
    return new Object[] {
      stationId,
      "Station " + stationId,
      "S" + stationId,
      "Address " + stationId,
      53.3498,
      -6.2603,
      capacity,
      "DUBLIN_CITY",
      availableBikes,
      availableDocks,
      0,
      0,
      true,
      true,
      true,
      now,
      now
    };
  }

  /**
   * Builds a network summary row matching findNetworkSummary column order: 0:total_stations,
   * 1:total_bikes, 2:total_docks, 3:disabled_bikes, 4:disabled_docks, 5:empty_stations,
   * 6:full_stations, 7:avg_fullness, 8:latest_timestamp
   */
  private Object[] buildNetworkSummaryRow(
      int totalStations,
      int totalBikes,
      int totalDocks,
      int disabledBikes,
      int disabledDocks,
      int emptyStations,
      int fullStations,
      double avgFullness) {
    return new Object[] {
      totalStations,
      totalBikes,
      totalDocks,
      disabledBikes,
      disabledDocks,
      emptyStations,
      fullStations,
      avgFullness,
      Timestamp.from(Instant.now())
    };
  }

  /**
   * Builds a region metrics row matching findRegionMetrics column order: 0:region_id,
   * 1:station_count, 2:total_capacity, 3:avg_usage_rate, 4:avg_available_bikes,
   * 5:avg_available_docks, 6:empty_stations, 7:full_stations
   */
  private Object[] buildRegionRow(
      String regionId, long stationCount, long capacity, double usageRate) {
    return new Object[] {regionId, stationCount, capacity, usageRate, 12.0, 18.0, 1L, 0L};
  }

  /**
   * Builds a time-series row matching column order: 0:period, 1:avg_available_bikes,
   * 2:avg_available_docks, 3:usage_rate_pct
   */
  private Object[] buildTimeSeriesRow(double avgBikes, double avgDocks, double usageRate) {
    return new Object[] {Timestamp.from(Instant.now()), avgBikes, avgDocks, usageRate};
  }

  /** Builds a ranking row matching column order: 0:station_id, 1:name, 2:avg_usage_rate */
  private Object[] buildRankingRow(int stationId, double avgUsageRate) {
    return new Object[] {stationId, "Station " + stationId, avgUsageRate};
  }

  /**
   * Builds an OD pair row matching findODPairs column order: 0:origin_station_id, 1:origin_name,
   * 2:origin_lat, 3:origin_lon, 4:dest_station_id, 5:dest_name, 6:dest_lat, 7:dest_lon,
   * 8:estimated_trips
   */
  private Object[] buildODPairRow(int originId, int destId, long trips) {
    return new Object[] {
      originId,
      "Station " + originId,
      53.3498,
      -6.2603,
      destId,
      "Station " + destId,
      53.3510,
      -6.2590,
      trips
    };
  }

  // =========================================================
  // getLiveStations
  // =========================================================
  @Nested
  @DisplayName("getLiveStations")
  class GetLiveStationsTests {

    @Test
    @DisplayName("maps snapshot rows to StationLiveDTOs")
    void getLiveStations_mapsRowsToDto() {
      Object[] row1 = buildSnapshotRow(1, 10, 20, 30);
      Object[] row2 = buildSnapshotRow(2, 0, 30, 30);
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(rows(row1, row2));

      List<StationLiveDTO> result = service.getLiveStations();

      assertEquals(2, result.size());
      StationLiveDTO station1 = result.get(0);
      assertEquals(1, station1.getStationId());
      assertEquals("Station 1", station1.getName());
      assertEquals(10, station1.getAvailableBikes());
      assertEquals(20, station1.getAvailableDocks());
      assertFalse(station1.getIsEmpty());
      assertFalse(station1.getIsFull());
    }

    @Test
    @DisplayName("sets isEmpty=true when availableBikes is 0")
    void getLiveStations_emptyStation_setsIsEmpty() {
      Object[] row = buildSnapshotRow(5, 0, 30, 30);
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(rows(row));

      List<StationLiveDTO> result = service.getLiveStations();

      assertTrue(result.get(0).getIsEmpty());
      assertFalse(result.get(0).getIsFull());
    }

    @Test
    @DisplayName("sets isFull=true when availableDocks is 0")
    void getLiveStations_fullStation_setsIsFull() {
      Object[] row = buildSnapshotRow(6, 30, 0, 30);
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(rows(row));

      List<StationLiveDTO> result = service.getLiveStations();

      assertFalse(result.get(0).getIsEmpty());
      assertTrue(result.get(0).getIsFull());
    }

    @Test
    @DisplayName("returns GREEN status when bikeAvailabilityPct >= 40%")
    void getLiveStations_highBikeAvailability_setsGreenStatus() {
      // capacity=30, availableBikes=15 => 15/30 = 50% => GREEN
      Object[] row = buildSnapshotRow(1, 15, 15, 30);
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(rows(row));

      StationLiveDTO result = service.getLiveStations().get(0);

      assertEquals("GREEN", result.getStatusColor());
      assertTrue(result.getBikeAvailabilityPct() >= 40.0);
    }

    @Test
    @DisplayName("returns YELLOW status when bikeAvailabilityPct is between 20% and 39%")
    void getLiveStations_mediumBikeAvailability_setsYellowStatus() {
      // capacity=30, availableBikes=9 => 9/30 = 30% => YELLOW
      Object[] row = buildSnapshotRow(2, 9, 21, 30);
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(rows(row));

      StationLiveDTO result = service.getLiveStations().get(0);

      assertEquals("YELLOW", result.getStatusColor());
      assertTrue(result.getBikeAvailabilityPct() >= 20.0 && result.getBikeAvailabilityPct() < 40.0);
    }

    @Test
    @DisplayName("returns RED status when bikeAvailabilityPct < 20%")
    void getLiveStations_lowBikeAvailability_setsRedStatus() {
      // capacity=30, availableBikes=3 => 3/30 = 10% => RED
      Object[] row = buildSnapshotRow(3, 3, 27, 30);
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(rows(row));

      StationLiveDTO result = service.getLiveStations().get(0);

      assertEquals("RED", result.getStatusColor());
      assertTrue(result.getBikeAvailabilityPct() < 20.0);
    }

    @Test
    @DisplayName("returns empty list when no snapshot rows")
    void getLiveStations_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(Collections.emptyList());

      List<StationLiveDTO> result = service.getLiveStations();

      assertTrue(result.isEmpty());
    }
  }

  // =========================================================
  // getNetworkSummary
  // =========================================================
  @Nested
  @DisplayName("getNetworkSummary")
  class GetNetworkSummaryTests {

    @Test
    @DisplayName("maps summary row to NetworkSummaryDTO")
    void getNetworkSummary_mapsRowToDto() {
      Object[] row = buildNetworkSummaryRow(100, 500, 2500, 5, 3, 4, 2, 35.5);
      when(snapshotRepository.findNetworkSummary()).thenReturn(rows(row));

      NetworkSummaryDTO result = service.getNetworkSummary();

      assertEquals(100, result.getTotalStations());
      assertEquals(100, result.getActiveStations());
      assertEquals(500, result.getTotalBikesAvailable());
      assertEquals(2500, result.getTotalDocksAvailable());
      assertEquals(5, result.getTotalDisabledBikes());
      assertEquals(3, result.getTotalDisabledDocks());
      assertEquals(4, result.getEmptyStations());
      assertEquals(2, result.getFullStations());
      assertEquals(35.5, result.getAvgNetworkFullnessPct());
      assertEquals(6, result.getRebalancingNeedCount()); // emptyStations + fullStations
      assertNotNull(result.getDataAsOf());
    }

    @Test
    @DisplayName("rebalancingNeedCount equals emptyStations + fullStations")
    void getNetworkSummary_rebalancingNeedCount_isCorrect() {
      Object[] row = buildNetworkSummaryRow(50, 200, 800, 0, 0, 10, 5, 40.0);
      when(snapshotRepository.findNetworkSummary()).thenReturn(rows(row));

      NetworkSummaryDTO result = service.getNetworkSummary();

      assertEquals(15, result.getRebalancingNeedCount());
    }

    @Test
    @DisplayName("returns empty DTO when query returns null")
    void getNetworkSummary_nullRow_returnsEmptyDto() {
      when(snapshotRepository.findNetworkSummary()).thenReturn(null);

      NetworkSummaryDTO result = service.getNetworkSummary();

      assertNotNull(result);
      assertNull(result.getTotalStations());
    }
  }

  // =========================================================
  // getRegionMetrics
  // =========================================================
  @Nested
  @DisplayName("getRegionMetrics")
  class GetRegionMetricsTests {

    @Test
    @DisplayName("maps region rows to RegionMetricsDTOs")
    void getRegionMetrics_mapsRowsToDto() {
      Object[] row1 = buildRegionRow("DUBLIN_CITY", 20L, 600L, 40.0);
      Object[] row2 = buildRegionRow("DUBLIN_NORTH", 10L, 300L, 55.0);
      when(snapshotRepository.findRegionMetrics()).thenReturn(rows(row1, row2));

      List<RegionMetricsDTO> result = service.getRegionMetrics();

      assertEquals(2, result.size());
      assertEquals("DUBLIN_CITY", result.get(0).getRegionId());
      assertEquals(20L, result.get(0).getStationCount());
      assertEquals(600L, result.get(0).getTotalCapacity());
      assertEquals(40.0, result.get(0).getAvgUsageRate());
      assertEquals("DUBLIN_NORTH", result.get(1).getRegionId());
    }

    @Test
    @DisplayName("returns empty list when no region rows")
    void getRegionMetrics_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findRegionMetrics()).thenReturn(Collections.emptyList());

      assertTrue(service.getRegionMetrics().isEmpty());
    }
  }

  // =========================================================
  // getStationTimeSeries
  // =========================================================
  @Nested
  @DisplayName("getStationTimeSeries")
  class GetStationTimeSeriesTests {

    @Test
    @DisplayName("routes to hourly repository when granularity is 'hour'")
    void getStationTimeSeries_hourGranularity_callsHourlyRepo() {
      Instant from = Instant.now().minusSeconds(86400);
      Instant to = Instant.now();
      Object[] row = buildTimeSeriesRow(12.0, 18.0, 40.0);
      when(historyRepository.findHourlyTimeSeriesForStation(1, from, to)).thenReturn(rows(row));

      List<StationTimeSeriesDTO> result = service.getStationTimeSeries(1, "hour", from, to);

      assertEquals(1, result.size());
      assertEquals(12.0, result.get(0).getAvgAvailableBikes());
      verify(historyRepository).findHourlyTimeSeriesForStation(1, from, to);
      verify(historyRepository, never()).findDailyTimeSeriesForStation(any(), any(), any());
      verify(historyRepository, never()).findWeeklyTimeSeriesForStation(any(), any(), any());
    }

    @Test
    @DisplayName("routes to weekly repository when granularity is 'week'")
    void getStationTimeSeries_weekGranularity_callsWeeklyRepo() {
      Instant from = Instant.now().minusSeconds(86400 * 30L);
      Instant to = Instant.now();
      Object[] row = buildTimeSeriesRow(11.0, 19.0, 37.0);
      when(historyRepository.findWeeklyTimeSeriesForStation(2, from, to)).thenReturn(rows(row));

      List<StationTimeSeriesDTO> result = service.getStationTimeSeries(2, "week", from, to);

      assertEquals(1, result.size());
      verify(historyRepository).findWeeklyTimeSeriesForStation(2, from, to);
    }

    @Test
    @DisplayName("routes to daily repository for 'day' granularity (default)")
    void getStationTimeSeries_dayGranularity_callsDailyRepo() {
      Instant from = Instant.now().minusSeconds(86400 * 7L);
      Instant to = Instant.now();
      Object[] row = buildTimeSeriesRow(13.0, 17.0, 43.3);
      when(historyRepository.findDailyTimeSeriesForStation(3, from, to)).thenReturn(rows(row));

      List<StationTimeSeriesDTO> result = service.getStationTimeSeries(3, "day", from, to);

      assertEquals(1, result.size());
      verify(historyRepository).findDailyTimeSeriesForStation(3, from, to);
    }

    @Test
    @DisplayName("maps time-series row fields correctly")
    void getStationTimeSeries_mapsFieldsCorrectly() {
      Instant from = Instant.now().minusSeconds(3600);
      Instant to = Instant.now();
      Object[] row = buildTimeSeriesRow(15.5, 14.5, 51.7);
      when(historyRepository.findDailyTimeSeriesForStation(1, from, to)).thenReturn(rows(row));

      StationTimeSeriesDTO dto = service.getStationTimeSeries(1, "day", from, to).get(0);

      assertEquals(15.5, dto.getAvgAvailableBikes());
      assertEquals(14.5, dto.getAvgAvailableDocks());
      assertEquals(51.7, dto.getUsageRatePct());
      assertNotNull(dto.getPeriod());
    }
  }

  // =========================================================
  // getNetworkDailyTrend
  // =========================================================
  @Nested
  @DisplayName("getNetworkDailyTrend")
  class GetNetworkDailyTrendTests {

    @Test
    @DisplayName("calls findNetworkDailyTrend with a since date 30 days back")
    void getNetworkDailyTrend_callsRepoWithCorrectSince() {
      Object[] row = buildTimeSeriesRow(10.0, 20.0, 33.3);
      when(historyRepository.findNetworkDailyTrend(any(Instant.class))).thenReturn(rows(row));

      List<StationTimeSeriesDTO> result = service.getNetworkDailyTrend(30);

      assertEquals(1, result.size());
      verify(historyRepository).findNetworkDailyTrend(any(Instant.class));
    }

    @Test
    @DisplayName("returns empty list when no trend data available")
    void getNetworkDailyTrend_emptyResult() {
      when(historyRepository.findNetworkDailyTrend(any(Instant.class)))
          .thenReturn(Collections.emptyList());

      assertTrue(service.getNetworkDailyTrend(7).isEmpty());
    }
  }

  // =========================================================
  // getNetworkMonthlyTrend
  // =========================================================
  @Nested
  @DisplayName("getNetworkMonthlyTrend")
  class GetNetworkMonthlyTrendTests {

    @Test
    @DisplayName("calls findNetworkMonthlyTrend with a since date n*30 days back")
    void getNetworkMonthlyTrend_callsRepoWithCorrectSince() {
      Object[] row = buildTimeSeriesRow(11.0, 19.0, 36.7);
      when(historyRepository.findNetworkMonthlyTrend(any(Instant.class))).thenReturn(rows(row));

      List<StationTimeSeriesDTO> result = service.getNetworkMonthlyTrend(12);

      assertEquals(1, result.size());
      verify(historyRepository).findNetworkMonthlyTrend(any(Instant.class));
    }
  }

  // =========================================================
  // getHourlyUsageProfile
  // =========================================================
  @Nested
  @DisplayName("getHourlyUsageProfile")
  class GetHourlyUsageProfileTests {

    @Test
    @DisplayName("returns map of hour -> usage rate")
    void getHourlyUsageProfile_returnsMappedProfile() {
      Object[] row8 = new Object[] {8, 65.0};
      Object[] row9 = new Object[] {9, 78.0};
      when(historyRepository.findHourlyUsageProfile(any(Instant.class)))
          .thenReturn(rows(row8, row9));

      Map<Integer, Double> result = service.getHourlyUsageProfile(30);

      assertEquals(2, result.size());
      assertEquals(65.0, result.get(8));
      assertEquals(78.0, result.get(9));
    }

    @Test
    @DisplayName("returns empty map when no profile data")
    void getHourlyUsageProfile_emptyRows_returnsEmptyMap() {
      when(historyRepository.findHourlyUsageProfile(any(Instant.class)))
          .thenReturn(Collections.emptyList());

      assertTrue(service.getHourlyUsageProfile(7).isEmpty());
    }
  }

  // =========================================================
  // getWeeklyUsageProfile
  // =========================================================
  @Nested
  @DisplayName("getWeeklyUsageProfile")
  class GetWeeklyUsageProfileTests {

    @Test
    @DisplayName("returns map of day-of-week -> usage rate")
    void getWeeklyUsageProfile_returnsMappedProfile() {
      Object[] mon = new Object[] {2, 62.0};
      Object[] sat = new Object[] {7, 45.0};
      when(historyRepository.findWeeklyUsageProfile(any(Instant.class))).thenReturn(rows(mon, sat));

      Map<Integer, Double> result = service.getWeeklyUsageProfile(90);

      assertEquals(2, result.size());
      assertEquals(62.0, result.get(2));
      assertEquals(45.0, result.get(7));
    }
  }

  // =========================================================
  // getWeekdayVsWeekendUsage
  // =========================================================
  @Nested
  @DisplayName("getWeekdayVsWeekendUsage")
  class GetWeekdayVsWeekendUsageTests {

    @Test
    @DisplayName("returns map with weekday and weekend keys")
    void getWeekdayVsWeekendUsage_returnsMappedResult() {
      Object[] weekday = new Object[] {"weekday", 62.5};
      Object[] weekend = new Object[] {"weekend", 48.0};
      when(historyRepository.findWeekdayVsWeekendUsage(any(Instant.class)))
          .thenReturn(rows(weekday, weekend));

      Map<String, Double> result = service.getWeekdayVsWeekendUsage(90);

      assertEquals(2, result.size());
      assertEquals(62.5, result.get("weekday"));
      assertEquals(48.0, result.get("weekend"));
    }
  }

  // =========================================================
  // getBusiestStations
  // =========================================================
  @Nested
  @DisplayName("getBusiestStations")
  class GetBusiestStationsTests {

    @Test
    @DisplayName("maps ranking rows to StationRankingDTOs")
    void getBusiestStations_mapsRowsToDto() {
      Object[] row1 = buildRankingRow(1, 90.0);
      Object[] row2 = buildRankingRow(2, 75.0);
      when(historyRepository.findBusiestStations(eq(10))).thenReturn(rows(row1, row2));

      List<StationRankingDTO> result = service.getBusiestStations(10);

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getStationId());
      assertEquals(90.0, result.get(0).getAvgUsageRate());
    }

    @Test
    @DisplayName("passes correct limit to repository")
    void getBusiestStations_passesCorrectArgs() {
      when(historyRepository.findBusiestStations(eq(5))).thenReturn(Collections.emptyList());

      service.getBusiestStations(5);

      verify(historyRepository).findBusiestStations(eq(5));
    }
  }

  // =========================================================
  // getLeastUsedStations
  // =========================================================
  @Nested
  @DisplayName("getLeastUsedStations")
  class GetLeastUsedStationsTests {

    @Test
    @DisplayName("maps ranking rows to StationRankingDTOs")
    void getLeastUsedStations_mapsRowsToDto() {
      Object[] row = buildRankingRow(99, 5.0);
      when(historyRepository.findLeastUsedStations(eq(10))).thenReturn(rows(row));

      List<StationRankingDTO> result = service.getLeastUsedStations(10);

      assertEquals(1, result.size());
      assertEquals(99, result.get(0).getStationId());
      assertEquals(5.0, result.get(0).getAvgUsageRate());
    }
  }

  // =========================================================
  // getNetworkKpi
  // =========================================================
  @Nested
  @DisplayName("getNetworkKpi")
  class GetNetworkKpiTests {

    @Test
    @DisplayName("computes full KPI DTO from multiple repository calls")
    void getNetworkKpi_computesAllFields() {
      when(snapshotRepository.findNetworkImbalanceScore()).thenReturn(rows(new Object[] {0.25}));
      when(historyRepository.findAvgHourlyTurnoverRate()).thenReturn(rows(new Object[] {1.8}));
      when(historyRepository.findTotalTripEstimate(any(Instant.class), any(Instant.class)))
          .thenReturn(rows(new Object[] {3500L}));
      when(historyRepository.findWeekdayVsWeekendUsage(any(Instant.class)))
          .thenReturn(rows(new Object[] {"weekday", 62.5}, new Object[] {"weekend", 48.0}));
      when(historyRepository.findHourlyUsageProfile(any(Instant.class)))
          .thenReturn(rows(new Object[] {9, 78.0}, new Object[] {17, 82.0}));
      when(historyRepository.findNetworkDailyTrend(any(Instant.class)))
          .thenReturn(rows(buildTimeSeriesRow(12.0, 18.0, 40.0)));
      when(snapshotRepository.findNetworkSummary())
          .thenReturn(rows(buildNetworkSummaryRow(100, 500, 2500, 5, 3, 4, 2, 35.5)));

      NetworkKpiDTO result = service.getNetworkKpi();

      assertEquals(6, result.getRebalancingNeedCount()); // 4 empty + 2 full
      assertEquals(0.25, result.getNetworkImbalanceScore());
      assertEquals(1.8, result.getAvgHourlyTurnoverRate());
      assertEquals(3500L, result.getDailyTripsEstimate());
      assertEquals(62.5, result.getWeekdayAvgUsageRate());
      assertEquals(48.0, result.getWeekendAvgUsageRate());
      assertNotNull(result.getHourlyUsageProfile());
      assertEquals(78.0, result.getHourlyUsageProfile().get(9));
      assertNotNull(result.getDailyTrend());
      assertEquals(1, result.getDailyTrend().size());
    }

    @Test
    @DisplayName("returns zero rebalancing need when summary row is null")
    void getNetworkKpi_nullSummaryRow_zeroRebalancingNeed() {
      when(snapshotRepository.findNetworkImbalanceScore()).thenReturn(rows(new Object[] {0.0}));
      when(historyRepository.findAvgHourlyTurnoverRate()).thenReturn(rows(new Object[] {0.0}));
      when(historyRepository.findTotalTripEstimate(any(), any()))
          .thenReturn(rows(new Object[] {0L}));
      when(historyRepository.findWeekdayVsWeekendUsage(any())).thenReturn(Collections.emptyList());
      when(historyRepository.findHourlyUsageProfile(any())).thenReturn(Collections.emptyList());
      when(historyRepository.findNetworkDailyTrend(any())).thenReturn(Collections.emptyList());
      when(snapshotRepository.findNetworkSummary()).thenReturn(null);

      NetworkKpiDTO result = service.getNetworkKpi();

      assertEquals(0, result.getRebalancingNeedCount());
    }

    @Test
    @DisplayName("returns zero scores when imbalance, turnover, and trips rows are null")
    void getNetworkKpi_nullImbalanceAndTurnoverRows_returnsZeros() {
      when(snapshotRepository.findNetworkImbalanceScore()).thenReturn(null);
      when(historyRepository.findAvgHourlyTurnoverRate()).thenReturn(null);
      when(historyRepository.findTotalTripEstimate(any(), any())).thenReturn(null);
      when(historyRepository.findWeekdayVsWeekendUsage(any())).thenReturn(Collections.emptyList());
      when(historyRepository.findHourlyUsageProfile(any())).thenReturn(Collections.emptyList());
      when(historyRepository.findNetworkDailyTrend(any())).thenReturn(Collections.emptyList());
      when(snapshotRepository.findNetworkSummary()).thenReturn(null);

      NetworkKpiDTO result = service.getNetworkKpi();

      assertEquals(0.0, result.getNetworkImbalanceScore());
      assertEquals(0.0, result.getAvgHourlyTurnoverRate());
      assertEquals(0L, result.getDailyTripsEstimate());
    }

    @Test
    @DisplayName("weekday and weekend rates default to 0 when absent from results")
    void getNetworkKpi_missingDayTypeRows_defaultsToZero() {
      when(snapshotRepository.findNetworkImbalanceScore()).thenReturn(rows(new Object[] {0.1}));
      when(historyRepository.findAvgHourlyTurnoverRate()).thenReturn(rows(new Object[] {1.0}));
      when(historyRepository.findTotalTripEstimate(any(), any()))
          .thenReturn(rows(new Object[] {100L}));
      when(historyRepository.findWeekdayVsWeekendUsage(any())).thenReturn(Collections.emptyList());
      when(historyRepository.findHourlyUsageProfile(any())).thenReturn(Collections.emptyList());
      when(historyRepository.findNetworkDailyTrend(any())).thenReturn(Collections.emptyList());
      when(snapshotRepository.findNetworkSummary()).thenReturn(null);

      NetworkKpiDTO result = service.getNetworkKpi();

      assertEquals(0.0, result.getWeekdayAvgUsageRate());
      assertEquals(0.0, result.getWeekendAvgUsageRate());
    }
  }

  // =========================================================
  // getODHeatmap
  // =========================================================
  @Nested
  @DisplayName("getODHeatmap")
  class GetODHeatmapTests {

    @Test
    @DisplayName("returns mapped OD pairs ordered by estimated trips")
    void getODHeatmap_returnsMappedPairs() {
      when(historyRepository.findODPairs(any(Instant.class), any(Instant.class), eq(50)))
          .thenReturn(rows(buildODPairRow(1, 2, 200L), buildODPairRow(3, 4, 150L)));

      List<StationODPairDTO> result = service.getODHeatmap(50);

      assertEquals(2, result.size());

      StationODPairDTO first = result.get(0);
      assertEquals(1, first.getOriginStationId());
      assertEquals("Station 1", first.getOriginName());
      assertEquals(2, first.getDestStationId());
      assertEquals("Station 2", first.getDestName());
      assertEquals(200L, first.getEstimatedTrips());
      assertNotNull(first.getOriginLat());
      assertNotNull(first.getOriginLon());
      assertNotNull(first.getDestLat());
      assertNotNull(first.getDestLon());

      StationODPairDTO second = result.get(1);
      assertEquals(3, second.getOriginStationId());
      assertEquals(4, second.getDestStationId());
      assertEquals(150L, second.getEstimatedTrips());
    }

    @Test
    @DisplayName("returns empty list when no OD pairs found")
    void getODHeatmap_noPairs_returnsEmptyList() {
      when(historyRepository.findODPairs(any(Instant.class), any(Instant.class), eq(50)))
          .thenReturn(Collections.emptyList());

      List<StationODPairDTO> result = service.getODHeatmap(50);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("passes correct limit to repository and uses last calendar month window")
    void getODHeatmap_passesLimitToRepository() {
      when(historyRepository.findODPairs(any(Instant.class), any(Instant.class), eq(10)))
          .thenReturn(rows(buildODPairRow(1, 2, 100L)));

      service.getODHeatmap(10);

      verify(historyRepository).findODPairs(any(Instant.class), any(Instant.class), eq(10));
    }

    @Test
    @DisplayName("handles null lat/lon values gracefully")
    void getODHeatmap_nullCoordinates_handledGracefully() {
      Object[] rowWithNullCoords =
          new Object[] {1, "Station 1", null, null, 2, "Station 2", null, null, 50L};
      when(historyRepository.findODPairs(any(Instant.class), any(Instant.class), anyInt()))
          .thenReturn(rows(rowWithNullCoords));

      List<StationODPairDTO> result = service.getODHeatmap(50);

      assertEquals(1, result.size());
      assertNull(result.get(0).getOriginLat());
      assertNull(result.get(0).getOriginLon());
      assertNull(result.get(0).getDestLat());
      assertNull(result.get(0).getDestLon());
      assertEquals(50L, result.get(0).getEstimatedTrips());
    }
  }
}
