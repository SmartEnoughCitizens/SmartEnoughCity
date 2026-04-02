package com.trinity.hermes.indicators.cycle.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trinity.hermes.indicators.cycle.dto.HourlyNetworkProfileDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RebalanceSuggestionDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.StationClassificationDTO;
import com.trinity.hermes.indicators.cycle.dto.StationHourlyUsageDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.service.CycleMetricsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CycleMetricsController.class)
@Import(CycleMetricsControllerTest.TestSecurityConfig.class)
public class CycleMetricsControllerTest {

  @Autowired MockMvc mockMvc;
  @MockitoBean CycleMetricsService cycleMetricsService;

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
      return token -> {
        throw new JwtException("Not used in tests");
      };
    }
  }

  @BeforeEach
  void resetMocks() {
    Mockito.reset(cycleMetricsService);
  }

  // -------------------------------------------------------------------------
  // Builder helpers
  // -------------------------------------------------------------------------

  private StationLiveDTO buildStationLiveDTO(int stationId) {
    StationLiveDTO dto = new StationLiveDTO();
    dto.setStationId(stationId);
    dto.setName("Station " + stationId);
    dto.setShortName("S" + stationId);
    dto.setAddress("Address " + stationId);
    dto.setLatitude(new BigDecimal("53.3498"));
    dto.setLongitude(new BigDecimal("-6.2603"));
    dto.setCapacity(30);
    dto.setRegionId("DUBLIN_CITY");
    dto.setAvailableBikes(10);
    dto.setAvailableDocks(20);
    dto.setDisabledBikes(0);
    dto.setDisabledDocks(0);
    dto.setIsInstalled(true);
    dto.setIsRenting(true);
    dto.setIsReturning(true);
    dto.setLastReported(Instant.now());
    dto.setSnapshotTimestamp(Instant.now());
    dto.setBikeAvailabilityPct(33.3);
    dto.setDockAvailabilityPct(66.7);
    dto.setStatusColor("GREEN");
    dto.setIsEmpty(false);
    dto.setIsFull(false);
    return dto;
  }

  private NetworkSummaryDTO buildNetworkSummaryDTO() {
    NetworkSummaryDTO dto = new NetworkSummaryDTO();
    dto.setTotalStations(100);
    dto.setActiveStations(100);
    dto.setTotalBikesAvailable(500);
    dto.setTotalDocksAvailable(2500);
    dto.setTotalDisabledBikes(5);
    dto.setTotalDisabledDocks(3);
    dto.setEmptyStations(4);
    dto.setFullStations(2);
    dto.setAvgNetworkFullnessPct(35.5);
    dto.setRebalancingNeedCount(4);
    dto.setDataAsOf(Instant.now());
    return dto;
  }

  private StationRankingDTO buildRankingDTO(int stationId) {
    StationRankingDTO dto = new StationRankingDTO();
    dto.setStationId(stationId);
    dto.setName("Station " + stationId);
    dto.setAvgUsageRate(75.0);
    return dto;
  }

  private RegionMetricsDTO buildRegionMetricsDTO(String regionId) {
    RegionMetricsDTO dto = new RegionMetricsDTO();
    dto.setRegionId(regionId);
    dto.setStationCount(20L);
    dto.setTotalCapacity(600L);
    dto.setAvgUsageRate(40.0);
    dto.setAvgAvailableBikes(12.0);
    dto.setAvgAvailableDocks(18.0);
    dto.setEmptyStations(1L);
    dto.setFullStations(0L);
    return dto;
  }

  private RebalanceSuggestionDTO buildRebalancingDTO(int sourceId, int targetId) {
    RebalanceSuggestionDTO dto = new RebalanceSuggestionDTO();
    dto.setSourceStationId(sourceId);
    dto.setSourceName("Source " + sourceId);
    dto.setSourceLat(new BigDecimal("53.3498"));
    dto.setSourceLon(new BigDecimal("-6.2603"));
    dto.setSourceBikes(15);
    dto.setTargetStationId(targetId);
    dto.setTargetName("Target " + targetId);
    dto.setTargetLat(new BigDecimal("53.3510"));
    dto.setTargetLon(new BigDecimal("-6.2590"));
    dto.setTargetCapacity(20);
    dto.setDistanceKm(0.8);
    return dto;
  }

  private HourlyNetworkProfileDTO buildHourlyProfileDTO(int hour) {
    HourlyNetworkProfileDTO dto = new HourlyNetworkProfileDTO();
    dto.setHourOfDay(hour);
    dto.setAvgTurnover(65.0);
    dto.setStationCount(100L);
    return dto;
  }

  private StationClassificationDTO buildClassificationDTO(int stationId, String classification) {
    StationClassificationDTO dto = new StationClassificationDTO();
    dto.setStationId(stationId);
    dto.setName("Station " + stationId);
    dto.setPeakHour(8);
    dto.setPeakUsage(78.0);
    dto.setClassification(classification);
    return dto;
  }

  private StationODPairDTO buildODPairDTO(int originId, int destId) {
    StationODPairDTO dto = new StationODPairDTO();
    dto.setOriginStationId(originId);
    dto.setOriginName("Station " + originId);
    dto.setOriginLat(new BigDecimal("53.3498"));
    dto.setOriginLon(new BigDecimal("-6.2603"));
    dto.setDestStationId(destId);
    dto.setDestName("Station " + destId);
    dto.setDestLat(new BigDecimal("53.3510"));
    dto.setDestLon(new BigDecimal("-6.2590"));
    dto.setEstimatedTrips(120);
    dto.setDistanceKm(1.5);
    return dto;
  }

  private StationHourlyUsageDTO buildStationHourlyDTO(int stationId, int hour) {
    StationHourlyUsageDTO dto = new StationHourlyUsageDTO();
    dto.setStationId(stationId);
    dto.setName("Station " + stationId);
    dto.setHourOfDay(hour);
    dto.setAvgTurnover(72.5);
    return dto;
  }

  // =========================================================
  // GET /api/v1/cycle/stations/live
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/stations/live")
  class LiveStationsTests {

    @Test
    @DisplayName("200 with list of live stations")
    void getLiveStations_returnsOk() throws Exception {
      when(cycleMetricsService.getLiveStations())
          .thenReturn(List.of(buildStationLiveDTO(1), buildStationLiveDTO(2)));

      mockMvc
          .perform(get("/api/v1/cycle/stations/live"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].stationId").value(1))
          .andExpect(jsonPath("$[0].statusColor").value("GREEN"))
          .andExpect(jsonPath("$[1].stationId").value(2));

      verify(cycleMetricsService).getLiveStations();
    }

    @Test
    @DisplayName("200 with empty list when no stations available")
    void getLiveStations_emptyList_returnsOk() throws Exception {
      when(cycleMetricsService.getLiveStations()).thenReturn(List.of());

      mockMvc
          .perform(get("/api/v1/cycle/stations/live"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getLiveStations_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getLiveStations()).thenThrow(new RuntimeException("DB error"));

      mockMvc
          .perform(get("/api/v1/cycle/stations/live"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/network/summary
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/network/summary")
  class NetworkSummaryTests {

    @Test
    @DisplayName("200 with network summary data")
    void getNetworkSummary_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkSummary()).thenReturn(buildNetworkSummaryDTO());

      mockMvc
          .perform(get("/api/v1/cycle/network/summary"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalStations").value(100))
          .andExpect(jsonPath("$.totalBikesAvailable").value(500))
          .andExpect(jsonPath("$.emptyStations").value(4))
          .andExpect(jsonPath("$.fullStations").value(2))
          .andExpect(jsonPath("$.rebalancingNeedCount").value(4));

      verify(cycleMetricsService).getNetworkSummary();
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getNetworkSummary_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getNetworkSummary()).thenThrow(new RuntimeException("Query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/network/summary"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/regions
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/regions")
  class RegionMetricsTests {

    @Test
    @DisplayName("200 with region metrics list")
    void getRegionMetrics_returnsOk() throws Exception {
      when(cycleMetricsService.getRegionMetrics())
          .thenReturn(
              List.of(buildRegionMetricsDTO("DUBLIN_CITY"), buildRegionMetricsDTO("DUBLIN_NORTH")));

      mockMvc
          .perform(get("/api/v1/cycle/regions"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].regionId").value("DUBLIN_CITY"))
          .andExpect(jsonPath("$[0].stationCount").value(20))
          .andExpect(jsonPath("$[1].regionId").value("DUBLIN_NORTH"));

      verify(cycleMetricsService).getRegionMetrics();
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getRegionMetrics_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getRegionMetrics()).thenThrow(new RuntimeException("Query failed"));

      mockMvc.perform(get("/api/v1/cycle/regions")).andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/rankings/busiest
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/rankings/busiest")
  class BusiestStationsTests {

    @Test
    @DisplayName("200 with default limit=10")
    void getBusiestStations_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getBusiestStations(10))
          .thenReturn(List.of(buildRankingDTO(1), buildRankingDTO(2)));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/busiest"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].stationId").value(1))
          .andExpect(jsonPath("$[0].avgUsageRate").value(75.0));

      verify(cycleMetricsService).getBusiestStations(10);
    }

    @Test
    @DisplayName("200 with custom limit parameter")
    void getBusiestStations_customLimit_returnsOk() throws Exception {
      when(cycleMetricsService.getBusiestStations(5)).thenReturn(List.of(buildRankingDTO(10)));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/busiest").param("limit", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(cycleMetricsService).getBusiestStations(5);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getBusiestStations_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getBusiestStations(anyInt()))
          .thenThrow(new RuntimeException("DB error"));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/busiest"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/rankings/underused
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/rankings/underused")
  class UnderusedStationsTests {

    @Test
    @DisplayName("200 with default limit=10")
    void getLeastUsedStations_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getLeastUsedStations(10)).thenReturn(List.of(buildRankingDTO(50)));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/underused"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].stationId").value(50));

      verify(cycleMetricsService).getLeastUsedStations(10);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getLeastUsedStations_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getLeastUsedStations(anyInt()))
          .thenThrow(new RuntimeException("DB error"));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/underused"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/network/rebalancing
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/network/rebalancing")
  class RebalancingTests {

    @Test
    @DisplayName("200 with default limit=30")
    void getRebalancing_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getRebalancingSuggestions(30))
          .thenReturn(List.of(buildRebalancingDTO(10, 20), buildRebalancingDTO(11, 21)));

      mockMvc
          .perform(get("/api/v1/cycle/network/rebalancing"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].sourceStationId").value(10))
          .andExpect(jsonPath("$[0].targetStationId").value(20))
          .andExpect(jsonPath("$[0].distanceKm").value(0.8));

      verify(cycleMetricsService).getRebalancingSuggestions(30);
    }

    @Test
    @DisplayName("200 with custom limit parameter")
    void getRebalancing_customLimit_returnsOk() throws Exception {
      when(cycleMetricsService.getRebalancingSuggestions(10))
          .thenReturn(List.of(buildRebalancingDTO(5, 6)));

      mockMvc
          .perform(get("/api/v1/cycle/network/rebalancing").param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(cycleMetricsService).getRebalancingSuggestions(10);
    }

    @Test
    @DisplayName("200 with empty list when no suggestions")
    void getRebalancing_empty_returnsOk() throws Exception {
      when(cycleMetricsService.getRebalancingSuggestions(30)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/v1/cycle/network/rebalancing"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getRebalancing_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getRebalancingSuggestions(anyInt()))
          .thenThrow(new RuntimeException("DB error"));

      mockMvc
          .perform(get("/api/v1/cycle/network/rebalancing"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/demand/network-hourly
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/demand/network-hourly")
  class NetworkHourlyProfileTests {

    @Test
    @DisplayName("200 with default days=30")
    void getNetworkHourly_defaultDays_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkHourlyProfile(30))
          .thenReturn(List.of(buildHourlyProfileDTO(8), buildHourlyProfileDTO(17)));

      mockMvc
          .perform(get("/api/v1/cycle/demand/network-hourly"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].hourOfDay").value(8))
          .andExpect(jsonPath("$[0].avgUsageRate").value(65.0))
          .andExpect(jsonPath("$[0].stationCount").value(100));

      verify(cycleMetricsService).getNetworkHourlyProfile(30);
    }

    @Test
    @DisplayName("200 with custom days parameter")
    void getNetworkHourly_customDays_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkHourlyProfile(7))
          .thenReturn(List.of(buildHourlyProfileDTO(9)));

      mockMvc
          .perform(get("/api/v1/cycle/demand/network-hourly").param("days", "7"))
          .andExpect(status().isOk());

      verify(cycleMetricsService).getNetworkHourlyProfile(7);
    }

    @Test
    @DisplayName("200 with empty list when no data")
    void getNetworkHourly_emptyResult_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkHourlyProfile(30)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/v1/cycle/demand/network-hourly"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getNetworkHourly_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getNetworkHourlyProfile(anyInt()))
          .thenThrow(new RuntimeException("Query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/demand/network-hourly"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/demand/classification
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/demand/classification")
  class StationClassificationTests {

    @Test
    @DisplayName("200 with default days=30")
    void getClassification_defaultDays_returnsOk() throws Exception {
      when(cycleMetricsService.getStationClassification(30))
          .thenReturn(
              List.of(
                  buildClassificationDTO(1, "MORNING_PEAK"),
                  buildClassificationDTO(2, "EVENING_PEAK")));

      mockMvc
          .perform(get("/api/v1/cycle/demand/classification"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].stationId").value(1))
          .andExpect(jsonPath("$[0].classification").value("MORNING_PEAK"))
          .andExpect(jsonPath("$[1].classification").value("EVENING_PEAK"));

      verify(cycleMetricsService).getStationClassification(30);
    }

    @Test
    @DisplayName("200 with custom days parameter")
    void getClassification_customDays_returnsOk() throws Exception {
      when(cycleMetricsService.getStationClassification(90))
          .thenReturn(List.of(buildClassificationDTO(5, "OFF_PEAK")));

      mockMvc
          .perform(get("/api/v1/cycle/demand/classification").param("days", "90"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].classification").value("OFF_PEAK"));

      verify(cycleMetricsService).getStationClassification(90);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getClassification_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getStationClassification(anyInt()))
          .thenThrow(new RuntimeException("Query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/demand/classification"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/demand/od-pairs
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/demand/od-pairs")
  class ODPairsTests {

    @Test
    @DisplayName("200 with default days=30, limit=50")
    void getODPairs_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getODPairs(30, 50))
          .thenReturn(List.of(buildODPairDTO(1, 2), buildODPairDTO(3, 4)));

      mockMvc
          .perform(get("/api/v1/cycle/demand/od-pairs"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].originStationId").value(1))
          .andExpect(jsonPath("$[0].destStationId").value(2))
          .andExpect(jsonPath("$[0].estimatedTrips").value(120))
          .andExpect(jsonPath("$[0].distanceKm").value(1.5));

      verify(cycleMetricsService).getODPairs(30, 50);
    }

    @Test
    @DisplayName("200 with custom days and limit parameters")
    void getODPairs_customParams_returnsOk() throws Exception {
      when(cycleMetricsService.getODPairs(7, 20)).thenReturn(List.of(buildODPairDTO(5, 6)));

      mockMvc
          .perform(get("/api/v1/cycle/demand/od-pairs").param("days", "7").param("limit", "20"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(cycleMetricsService).getODPairs(7, 20);
    }

    @Test
    @DisplayName("200 with empty list when no OD pairs found")
    void getODPairs_emptyResult_returnsOk() throws Exception {
      when(cycleMetricsService.getODPairs(30, 50)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/v1/cycle/demand/od-pairs"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getODPairs_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getODPairs(anyInt(), anyInt()))
          .thenThrow(new RuntimeException("OD query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/demand/od-pairs"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/demand/station-hourly
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/demand/station-hourly")
  class StationHourlyUsageTests {

    @Test
    @DisplayName("200 with default days=30, limit=30")
    void getStationHourly_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getStationHourlyUsage(30, 30))
          .thenReturn(
              List.of(
                  buildStationHourlyDTO(1, 8),
                  buildStationHourlyDTO(1, 9),
                  buildStationHourlyDTO(2, 17)));

      mockMvc
          .perform(get("/api/v1/cycle/demand/station-hourly"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(3))
          .andExpect(jsonPath("$[0].stationId").value(1))
          .andExpect(jsonPath("$[0].hourOfDay").value(8))
          .andExpect(jsonPath("$[0].avgUsageRate").value(72.5))
          .andExpect(jsonPath("$[1].hourOfDay").value(9))
          .andExpect(jsonPath("$[2].stationId").value(2));

      verify(cycleMetricsService).getStationHourlyUsage(30, 30);
    }

    @Test
    @DisplayName("200 with custom days and limit parameters")
    void getStationHourly_customParams_returnsOk() throws Exception {
      when(cycleMetricsService.getStationHourlyUsage(7, 10))
          .thenReturn(List.of(buildStationHourlyDTO(5, 12)));

      mockMvc
          .perform(
              get("/api/v1/cycle/demand/station-hourly").param("days", "7").param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].stationId").value(5));

      verify(cycleMetricsService).getStationHourlyUsage(7, 10);
    }

    @Test
    @DisplayName("200 with empty list when no data")
    void getStationHourly_emptyResult_returnsOk() throws Exception {
      when(cycleMetricsService.getStationHourlyUsage(30, 30)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/v1/cycle/demand/station-hourly"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getStationHourly_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getStationHourlyUsage(anyInt(), anyInt()))
          .thenThrow(new RuntimeException("Query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/demand/station-hourly"))
          .andExpect(status().isInternalServerError());
    }
  }
}
