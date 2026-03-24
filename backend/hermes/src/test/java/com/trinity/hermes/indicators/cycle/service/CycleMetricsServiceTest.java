package com.trinity.hermes.indicators.cycle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinity.hermes.indicators.cycle.dto.HourlyNetworkProfileDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.RebalanceSuggestionDTO;
import com.trinity.hermes.indicators.cycle.dto.StationClassificationDTO;
import com.trinity.hermes.indicators.cycle.dto.StationHourlyUsageDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesSnapshotRepository;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesStationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

  @InjectMocks CycleMetricsService service;

  // -------------------------------------------------------------------------
  // Helper utilities
  // -------------------------------------------------------------------------

  @SafeVarargs
  private static List<Object[]> rows(Object[]... items) {
    List<Object[]> list = new ArrayList<>();
    for (Object[] item : items) list.add(item);
    return list;
  }

  /**
   * Snapshot row matching findLatestSnapshotPerStation columns:
   * 0:station_id 1:name 2:short_name 3:address 4:latitude 5:longitude 6:capacity 7:region_id
   * 8:available_bikes 9:available_docks 10:disabled_bikes 11:disabled_docks 12:is_installed
   * 13:is_renting 14:is_returning 15:last_reported 16:snapshot_timestamp
   */
  private Object[] buildSnapshotRow(int stationId, int availableBikes, int availableDocks, int capacity) {
    Timestamp now = Timestamp.from(Instant.now());
    return new Object[]{
        stationId, "Station " + stationId, "S" + stationId, "Address " + stationId,
        53.3498, -6.2603, capacity, "DUBLIN_CITY",
        availableBikes, availableDocks, 0, 0,
        true, true, true, now, now
    };
  }

  /**
   * Network summary row matching findNetworkSummary columns:
   * 0:total_stations 1:total_bikes 2:total_docks 3:disabled_bikes 4:disabled_docks
   * 5:empty_stations 6:full_stations 7:avg_fullness 8:latest_timestamp
   */
  private Object[] buildNetworkSummaryRow(int totalStations, int totalBikes, int totalDocks,
      int disabledBikes, int disabledDocks, int emptyStations, int fullStations, double avgFullness) {
    return new Object[]{
        totalStations, totalBikes, totalDocks, disabledBikes, disabledDocks,
        emptyStations, fullStations, avgFullness, Timestamp.from(Instant.now())
    };
  }

  /**
   * Region row matching findRegionMetrics columns:
   * 0:region_id 1:station_count 2:total_capacity 3:avg_usage_rate
   * 4:avg_available_bikes 5:avg_available_docks 6:empty_stations 7:full_stations
   */
  private Object[] buildRegionRow(String regionId, long stationCount, long capacity, double usageRate) {
    return new Object[]{regionId, stationCount, capacity, usageRate, 12.0, 18.0, 1L, 0L};
  }

  /** Ranking row matching findBusiestStations / findLeastUsedStations: 0:station_id 1:name 2:avg_usage_rate */
  private Object[] buildRankingRow(int stationId, double avgUsageRate) {
    return new Object[]{stationId, "Station " + stationId, avgUsageRate};
  }

  /**
   * Rebalancing row matching findRebalancingSuggestions columns:
   * 0:source_station_id 1:source_name 2:source_lat 3:source_lon 4:source_bikes
   * 5:target_station_id 6:target_name 7:target_lat 8:target_lon 9:target_capacity 10:distance_km
   */
  private Object[] buildRebalancingRow(int sourceId, int targetId) {
    return new Object[]{
        sourceId, "Source " + sourceId, 53.3498, -6.2603, 15,
        targetId, "Target " + targetId, 53.3510, -6.2590, 20, 0.8
    };
  }

  /**
   * Hourly network profile row matching findNetworkHourlyProfile:
   * 0:hour_of_day 1:avg_usage_rate 2:station_count
   */
  private Object[] buildHourlyProfileRow(int hour, double usageRate, long stationCount) {
    return new Object[]{hour, usageRate, stationCount};
  }

  /**
   * Station classification row matching findStationClassification:
   * 0:station_id 1:name 2:peak_hour 3:peak_usage 4:classification
   */
  private Object[] buildClassificationRow(int stationId, int peakHour, double peakUsage, String classification) {
    return new Object[]{stationId, "Station " + stationId, peakHour, peakUsage, classification};
  }

  /**
   * OD pair row matching findODPairs:
   * 0:origin_station_id 1:origin_name 2:origin_lat 3:origin_lon
   * 4:dest_station_id 5:dest_name 6:dest_lat 7:dest_lon 8:estimated_trips 9:distance_km
   */
  private Object[] buildODPairRow(int originId, int destId, int trips, double distanceKm) {
    return new Object[]{
        originId, "Station " + originId, 53.3498, -6.2603,
        destId, "Station " + destId, 53.3510, -6.2590,
        trips, distanceKm
    };
  }

  /**
   * Station hourly usage row matching findStationHourlyUsage:
   * 0:station_id 1:name 2:hour_of_day 3:avg_usage_rate
   */
  private Object[] buildStationHourlyRow(int stationId, int hour, double usageRate) {
    return new Object[]{stationId, "Station " + stationId, hour, usageRate};
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
      when(snapshotRepository.findLatestSnapshotPerStation())
          .thenReturn(rows(buildSnapshotRow(1, 10, 20, 30), buildSnapshotRow(2, 0, 30, 30)));

      List<StationLiveDTO> result = service.getLiveStations();

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getStationId());
      assertEquals("Station 1", result.get(0).getName());
      assertEquals(10, result.get(0).getAvailableBikes());
      assertEquals(20, result.get(0).getAvailableDocks());
      assertFalse(result.get(0).getIsEmpty());
    }

    @Test
    @DisplayName("sets isEmpty=true when availableBikes is 0")
    void getLiveStations_emptyStation_setsIsEmpty() {
      when(snapshotRepository.findLatestSnapshotPerStation())
          .thenReturn(rows(buildSnapshotRow(5, 0, 30, 30)));

      assertTrue(service.getLiveStations().get(0).getIsEmpty());
      assertFalse(service.getLiveStations().get(0).getIsFull());
    }

    @Test
    @DisplayName("sets isFull=true when availableDocks is 0")
    void getLiveStations_fullStation_setsIsFull() {
      when(snapshotRepository.findLatestSnapshotPerStation())
          .thenReturn(rows(buildSnapshotRow(6, 30, 0, 30)));

      assertFalse(service.getLiveStations().get(0).getIsEmpty());
      assertTrue(service.getLiveStations().get(0).getIsFull());
    }

    @Test
    @DisplayName("returns GREEN status when bikeAvailabilityPct >= 40%")
    void getLiveStations_highBikeAvailability_setsGreenStatus() {
      // capacity=30, availableBikes=15 => 50% => GREEN
      when(snapshotRepository.findLatestSnapshotPerStation())
          .thenReturn(rows(buildSnapshotRow(1, 15, 15, 30)));

      StationLiveDTO result = service.getLiveStations().get(0);

      assertEquals("GREEN", result.getStatusColor());
      assertTrue(result.getBikeAvailabilityPct() >= 40.0);
    }

    @Test
    @DisplayName("returns YELLOW status when bikeAvailabilityPct is between 20% and 39%")
    void getLiveStations_mediumBikeAvailability_setsYellowStatus() {
      // capacity=30, availableBikes=9 => 30% => YELLOW
      when(snapshotRepository.findLatestSnapshotPerStation())
          .thenReturn(rows(buildSnapshotRow(2, 9, 21, 30)));

      StationLiveDTO result = service.getLiveStations().get(0);

      assertEquals("YELLOW", result.getStatusColor());
    }

    @Test
    @DisplayName("returns RED status when bikeAvailabilityPct < 20%")
    void getLiveStations_lowBikeAvailability_setsRedStatus() {
      // capacity=30, availableBikes=3 => 10% => RED
      when(snapshotRepository.findLatestSnapshotPerStation())
          .thenReturn(rows(buildSnapshotRow(3, 3, 27, 30)));

      assertEquals("RED", service.getLiveStations().get(0).getStatusColor());
    }

    @Test
    @DisplayName("returns empty list when no snapshot rows")
    void getLiveStations_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findLatestSnapshotPerStation()).thenReturn(Collections.emptyList());

      assertTrue(service.getLiveStations().isEmpty());
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
      when(snapshotRepository.findNetworkSummary())
          .thenReturn(rows(buildNetworkSummaryRow(100, 500, 2500, 5, 3, 4, 2, 35.5)));

      NetworkSummaryDTO result = service.getNetworkSummary();

      assertEquals(100, result.getTotalStations());
      assertEquals(500, result.getTotalBikesAvailable());
      assertEquals(2500, result.getTotalDocksAvailable());
      assertEquals(5, result.getTotalDisabledBikes());
      assertEquals(4, result.getEmptyStations());
      assertEquals(2, result.getFullStations());
      assertEquals(35.5, result.getAvgNetworkFullnessPct());
      assertEquals(4, result.getRebalancingNeedCount()); // emptyStations
      assertNotNull(result.getDataAsOf());
    }

    @Test
    @DisplayName("rebalancingNeedCount equals emptyStations only")
    void getNetworkSummary_rebalancingNeedCount_isEmptyStations() {
      when(snapshotRepository.findNetworkSummary())
          .thenReturn(rows(buildNetworkSummaryRow(50, 200, 800, 0, 0, 10, 5, 40.0)));

      assertEquals(10, service.getNetworkSummary().getRebalancingNeedCount());
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
      when(snapshotRepository.findRegionMetrics()).thenReturn(
          rows(buildRegionRow("DUBLIN_CITY", 20L, 600L, 40.0),
              buildRegionRow("DUBLIN_NORTH", 10L, 300L, 55.0)));

      List<RegionMetricsDTO> result = service.getRegionMetrics();

      assertEquals(2, result.size());
      assertEquals("DUBLIN_CITY", result.get(0).getRegionId());
      assertEquals(20L, result.get(0).getStationCount());
      assertEquals(40.0, result.get(0).getAvgUsageRate());
      assertEquals("DUBLIN_NORTH", result.get(1).getRegionId());
    }

    @Test
    @DisplayName("returns empty list when no rows")
    void getRegionMetrics_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findRegionMetrics()).thenReturn(Collections.emptyList());

      assertTrue(service.getRegionMetrics().isEmpty());
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
      when(snapshotRepository.findBusiestStations(10))
          .thenReturn(rows(buildRankingRow(1, 90.0), buildRankingRow(2, 75.0)));

      List<StationRankingDTO> result = service.getBusiestStations(10);

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getStationId());
      assertEquals(90.0, result.get(0).getAvgUsageRate());
    }

    @Test
    @DisplayName("passes correct limit to repository")
    void getBusiestStations_passesLimit() {
      when(snapshotRepository.findBusiestStations(5)).thenReturn(Collections.emptyList());

      service.getBusiestStations(5);

      verify(snapshotRepository).findBusiestStations(5);
    }

    @Test
    @DisplayName("returns empty list when no data")
    void getBusiestStations_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findBusiestStations(10)).thenReturn(Collections.emptyList());

      assertTrue(service.getBusiestStations(10).isEmpty());
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
      when(snapshotRepository.findLeastUsedStations(10))
          .thenReturn(rows(buildRankingRow(99, 5.0)));

      List<StationRankingDTO> result = service.getLeastUsedStations(10);

      assertEquals(1, result.size());
      assertEquals(99, result.get(0).getStationId());
      assertEquals(5.0, result.get(0).getAvgUsageRate());
    }

    @Test
    @DisplayName("passes correct limit to repository")
    void getLeastUsedStations_passesLimit() {
      when(snapshotRepository.findLeastUsedStations(3)).thenReturn(Collections.emptyList());

      service.getLeastUsedStations(3);

      verify(snapshotRepository).findLeastUsedStations(3);
    }
  }

  // =========================================================
  // getRebalancingSuggestions
  // =========================================================
  @Nested
  @DisplayName("getRebalancingSuggestions")
  class GetRebalancingSuggestionsTests {

    @Test
    @DisplayName("maps rebalancing rows to RebalanceSuggestionDTOs")
    void getRebalancingSuggestions_mapsRowsToDto() {
      when(snapshotRepository.findRebalancingSuggestions(30))
          .thenReturn(rows(buildRebalancingRow(10, 20), buildRebalancingRow(11, 21)));

      List<RebalanceSuggestionDTO> result = service.getRebalancingSuggestions(30);

      assertEquals(2, result.size());
      assertEquals(10, result.get(0).getSourceStationId());
      assertEquals("Source 10", result.get(0).getSourceName());
      assertEquals(20, result.get(0).getTargetStationId());
      assertEquals("Target 20", result.get(0).getTargetName());
      assertEquals(15, result.get(0).getSourceBikes());
      assertEquals(20, result.get(0).getTargetCapacity());
      assertNotNull(result.get(0).getSourceLat());
      assertNotNull(result.get(0).getDistanceKm());
    }

    @Test
    @DisplayName("returns empty list when no suggestions")
    void getRebalancingSuggestions_empty_returnsEmptyList() {
      when(snapshotRepository.findRebalancingSuggestions(30)).thenReturn(Collections.emptyList());

      assertTrue(service.getRebalancingSuggestions(30).isEmpty());
    }

    @Test
    @DisplayName("passes correct limit to repository")
    void getRebalancingSuggestions_passesLimit() {
      when(snapshotRepository.findRebalancingSuggestions(10)).thenReturn(Collections.emptyList());

      service.getRebalancingSuggestions(10);

      verify(snapshotRepository).findRebalancingSuggestions(10);
    }
  }

  // =========================================================
  // getNetworkHourlyProfile
  // =========================================================
  @Nested
  @DisplayName("getNetworkHourlyProfile")
  class GetNetworkHourlyProfileTests {

    @Test
    @DisplayName("maps rows to HourlyNetworkProfileDTOs")
    void getNetworkHourlyProfile_mapsRowsToDto() {
      when(snapshotRepository.findNetworkHourlyProfile(30)).thenReturn(
          rows(buildHourlyProfileRow(8, 65.0, 100L), buildHourlyProfileRow(17, 82.0, 100L)));

      List<HourlyNetworkProfileDTO> result = service.getNetworkHourlyProfile(30);

      assertEquals(2, result.size());
      assertEquals(8, result.get(0).getHourOfDay());
      assertEquals(65.0, result.get(0).getAvgUsageRate());
      assertEquals(100L, result.get(0).getStationCount());
      assertEquals(17, result.get(1).getHourOfDay());
      assertEquals(82.0, result.get(1).getAvgUsageRate());
    }

    @Test
    @DisplayName("returns empty list when no data")
    void getNetworkHourlyProfile_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findNetworkHourlyProfile(7)).thenReturn(Collections.emptyList());

      assertTrue(service.getNetworkHourlyProfile(7).isEmpty());
    }

    @Test
    @DisplayName("passes correct days parameter to repository")
    void getNetworkHourlyProfile_passesCorrectDays() {
      when(snapshotRepository.findNetworkHourlyProfile(90)).thenReturn(Collections.emptyList());

      service.getNetworkHourlyProfile(90);

      verify(snapshotRepository).findNetworkHourlyProfile(90);
    }
  }

  // =========================================================
  // getStationClassification
  // =========================================================
  @Nested
  @DisplayName("getStationClassification")
  class GetStationClassificationTests {

    @Test
    @DisplayName("maps rows to StationClassificationDTOs")
    void getStationClassification_mapsRowsToDto() {
      when(snapshotRepository.findStationClassification(30)).thenReturn(
          rows(buildClassificationRow(1, 8, 78.0, "MORNING_PEAK"),
              buildClassificationRow(2, 17, 82.0, "EVENING_PEAK")));

      List<StationClassificationDTO> result = service.getStationClassification(30);

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getStationId());
      assertEquals(8, result.get(0).getPeakHour());
      assertEquals(78.0, result.get(0).getPeakUsage());
      assertEquals("MORNING_PEAK", result.get(0).getClassification());
      assertEquals("EVENING_PEAK", result.get(1).getClassification());
    }

    @Test
    @DisplayName("returns empty list when no data")
    void getStationClassification_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findStationClassification(30)).thenReturn(Collections.emptyList());

      assertTrue(service.getStationClassification(30).isEmpty());
    }

    @Test
    @DisplayName("passes correct days to repository")
    void getStationClassification_passesCorrectDays() {
      when(snapshotRepository.findStationClassification(7)).thenReturn(Collections.emptyList());

      service.getStationClassification(7);

      verify(snapshotRepository).findStationClassification(7);
    }
  }

  // =========================================================
  // getODPairs
  // =========================================================
  @Nested
  @DisplayName("getODPairs")
  class GetODPairsTests {

    @Test
    @DisplayName("maps rows to StationODPairDTOs")
    void getODPairs_mapsRowsToDto() {
      when(snapshotRepository.findODPairs(30, 50)).thenReturn(
          rows(buildODPairRow(1, 2, 120, 1.5), buildODPairRow(3, 4, 80, 2.0)));

      List<StationODPairDTO> result = service.getODPairs(30, 50);

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getOriginStationId());
      assertEquals("Station 1", result.get(0).getOriginName());
      assertEquals(2, result.get(0).getDestStationId());
      assertEquals(120, result.get(0).getEstimatedTrips());
      assertEquals(1.5, result.get(0).getDistanceKm());
      assertNotNull(result.get(0).getOriginLat());
      assertNotNull(result.get(0).getDestLon());
    }

    @Test
    @DisplayName("returns empty list when no OD pairs found")
    void getODPairs_empty_returnsEmptyList() {
      when(snapshotRepository.findODPairs(30, 50)).thenReturn(Collections.emptyList());

      assertTrue(service.getODPairs(30, 50).isEmpty());
    }

    @Test
    @DisplayName("passes correct days and limit to repository")
    void getODPairs_passesCorrectArgs() {
      when(snapshotRepository.findODPairs(7, 20)).thenReturn(Collections.emptyList());

      service.getODPairs(7, 20);

      verify(snapshotRepository).findODPairs(7, 20);
    }

    @Test
    @DisplayName("handles null lat/lon values gracefully")
    void getODPairs_nullCoordinates_handledGracefully() {
      Object[] rowWithNullCoords = new Object[]{
          1, "Station 1", null, null, 2, "Station 2", null, null, 50, 0.9
      };
      when(snapshotRepository.findODPairs(30, 50)).thenReturn(rows(rowWithNullCoords));

      List<StationODPairDTO> result = service.getODPairs(30, 50);

      assertEquals(1, result.size());
      assertNull(result.get(0).getOriginLat());
      assertNull(result.get(0).getDestLon());
      assertEquals(50, result.get(0).getEstimatedTrips());
    }
  }

  // =========================================================
  // getStationHourlyUsage
  // =========================================================
  @Nested
  @DisplayName("getStationHourlyUsage")
  class GetStationHourlyUsageTests {

    @Test
    @DisplayName("maps rows to StationHourlyUsageDTOs")
    void getStationHourlyUsage_mapsRowsToDto() {
      when(snapshotRepository.findStationHourlyUsage(30, 30)).thenReturn(
          rows(buildStationHourlyRow(1, 8, 72.5),
              buildStationHourlyRow(1, 9, 88.0),
              buildStationHourlyRow(2, 17, 60.0)));

      List<StationHourlyUsageDTO> result = service.getStationHourlyUsage(30, 30);

      assertEquals(3, result.size());
      assertEquals(1, result.get(0).getStationId());
      assertEquals("Station 1", result.get(0).getName());
      assertEquals(8, result.get(0).getHourOfDay());
      assertEquals(72.5, result.get(0).getAvgUsageRate());
      assertEquals(9, result.get(1).getHourOfDay());
      assertEquals(88.0, result.get(1).getAvgUsageRate());
      assertEquals(2, result.get(2).getStationId());
      assertEquals(17, result.get(2).getHourOfDay());
    }

    @Test
    @DisplayName("returns empty list when no data")
    void getStationHourlyUsage_emptyRows_returnsEmptyList() {
      when(snapshotRepository.findStationHourlyUsage(30, 30)).thenReturn(Collections.emptyList());

      assertTrue(service.getStationHourlyUsage(30, 30).isEmpty());
    }

    @Test
    @DisplayName("passes correct days and stationLimit to repository")
    void getStationHourlyUsage_passesCorrectArgs() {
      when(snapshotRepository.findStationHourlyUsage(7, 10)).thenReturn(Collections.emptyList());

      service.getStationHourlyUsage(7, 10);

      verify(snapshotRepository).findStationHourlyUsage(7, 10);
    }
  }
}
