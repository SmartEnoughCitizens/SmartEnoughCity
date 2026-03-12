package com.trinity.hermes.indicators.cycle.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trinity.hermes.indicators.cycle.dto.NetworkKpiDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.StationEventDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.dto.StationTimeSeriesDTO;
import com.trinity.hermes.indicators.cycle.service.CycleMetricsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
  // Helper builders
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
    dto.setFullnessPct(33.3);
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
    dto.setRebalancingNeedCount(6);
    dto.setDataAsOf(Instant.now());
    return dto;
  }

  private StationTimeSeriesDTO buildTimeSeriesDTO() {
    StationTimeSeriesDTO dto = new StationTimeSeriesDTO();
    dto.setPeriod(Instant.now());
    dto.setAvgAvailableBikes(12.5);
    dto.setAvgAvailableDocks(17.5);
    dto.setUsageRatePct(41.7);
    return dto;
  }

  private StationRankingDTO buildRankingDTO(int stationId) {
    StationRankingDTO dto = new StationRankingDTO();
    dto.setStationId(stationId);
    dto.setName("Station " + stationId);
    dto.setRegionId("DUBLIN_CITY");
    dto.setCapacity(30);
    dto.setAvgUsageRate(75.0);
    dto.setAvgAvailableBikes(7.5);
    dto.setAvgAvailableDocks(22.5);
    dto.setEmptyEventCount(2L);
    dto.setFullEventCount(1L);
    return dto;
  }

  private StationEventDTO buildEventDTO(int stationId, String eventType) {
    StationEventDTO dto = new StationEventDTO();
    dto.setStationId(stationId);
    dto.setStationName("Station " + stationId);
    dto.setEventTime(Instant.now());
    dto.setAvailableBikes(eventType.equals("EMPTY") ? 0 : 5);
    dto.setPrevAvailableBikes(eventType.equals("EMPTY") ? 2 : 0);
    dto.setEventType(eventType);
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
      when(cycleMetricsService.getLiveStations())
          .thenThrow(new RuntimeException("Database unavailable"));

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
          .andExpect(jsonPath("$.rebalancingNeedCount").value(6));

      verify(cycleMetricsService).getNetworkSummary();
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getNetworkSummary_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getNetworkSummary())
          .thenThrow(new RuntimeException("Query failed"));

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
      when(cycleMetricsService.getRegionMetrics())
          .thenThrow(new RuntimeException("Query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/regions"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/stations/{stationId}/history
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/stations/{stationId}/history")
  class StationHistoryTests {

    @Test
    @DisplayName("200 with default granularity (day) and default date range")
    void getStationHistory_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getStationTimeSeries(eq(1), eq("day"), any(Instant.class), any(Instant.class)))
          .thenReturn(List.of(buildTimeSeriesDTO()));

      mockMvc
          .perform(get("/api/v1/cycle/stations/1/history"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].avgAvailableBikes").value(12.5));

      verify(cycleMetricsService)
          .getStationTimeSeries(eq(1), eq("day"), any(Instant.class), any(Instant.class));
    }

    @Test
    @DisplayName("200 with hourly granularity")
    void getStationHistory_hourlyGranularity_returnsOk() throws Exception {
      when(cycleMetricsService.getStationTimeSeries(eq(5), eq("hour"), any(Instant.class), any(Instant.class)))
          .thenReturn(List.of(buildTimeSeriesDTO(), buildTimeSeriesDTO()));

      mockMvc
          .perform(get("/api/v1/cycle/stations/5/history").param("granularity", "hour"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("200 with weekly granularity")
    void getStationHistory_weeklyGranularity_returnsOk() throws Exception {
      when(cycleMetricsService.getStationTimeSeries(eq(3), eq("week"), any(Instant.class), any(Instant.class)))
          .thenReturn(List.of(buildTimeSeriesDTO()));

      mockMvc
          .perform(get("/api/v1/cycle/stations/3/history").param("granularity", "week"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getStationHistory_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getStationTimeSeries(anyInt(), anyString(), any(), any()))
          .thenThrow(new RuntimeException("History query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/stations/1/history"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/trends/hourly
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/trends/hourly")
  class HourlyTrendTests {

    @Test
    @DisplayName("200 with default 30-day window")
    void getHourlyProfile_defaultDays_returnsOk() throws Exception {
      Map<Integer, Double> profile =
          Map.of(8, 65.0, 9, 78.0, 17, 82.0, 18, 71.0);
      when(cycleMetricsService.getHourlyUsageProfile(30)).thenReturn(profile);

      mockMvc
          .perform(get("/api/v1/cycle/trends/hourly"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.8").value(65.0))
          .andExpect(jsonPath("$.9").value(78.0));

      verify(cycleMetricsService).getHourlyUsageProfile(30);
    }

    @Test
    @DisplayName("200 with custom days parameter")
    void getHourlyProfile_customDays_returnsOk() throws Exception {
      when(cycleMetricsService.getHourlyUsageProfile(7)).thenReturn(Map.of(9, 55.0));

      mockMvc
          .perform(get("/api/v1/cycle/trends/hourly").param("days", "7"))
          .andExpect(status().isOk());

      verify(cycleMetricsService).getHourlyUsageProfile(7);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getHourlyProfile_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getHourlyUsageProfile(anyInt()))
          .thenThrow(new RuntimeException("Profile query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/trends/hourly"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/trends/weekly
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/trends/weekly")
  class WeeklyTrendTests {

    @Test
    @DisplayName("200 with default 90-day window")
    void getWeeklyProfile_defaultDays_returnsOk() throws Exception {
      when(cycleMetricsService.getWeeklyUsageProfile(90))
          .thenReturn(Map.of(1, 60.0, 2, 62.0, 6, 45.0, 7, 40.0));

      mockMvc
          .perform(get("/api/v1/cycle/trends/weekly"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.1").value(60.0));

      verify(cycleMetricsService).getWeeklyUsageProfile(90);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getWeeklyProfile_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getWeeklyUsageProfile(anyInt()))
          .thenThrow(new RuntimeException("Weekly profile failed"));

      mockMvc
          .perform(get("/api/v1/cycle/trends/weekly"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/trends/weekday-vs-weekend
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/trends/weekday-vs-weekend")
  class WeekdayVsWeekendTests {

    @Test
    @DisplayName("200 with weekday and weekend rates")
    void getWeekdayVsWeekend_returnsOk() throws Exception {
      when(cycleMetricsService.getWeekdayVsWeekendUsage(90))
          .thenReturn(Map.of("weekday", 62.5, "weekend", 48.0));

      mockMvc
          .perform(get("/api/v1/cycle/trends/weekday-vs-weekend"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.weekday").value(62.5))
          .andExpect(jsonPath("$.weekend").value(48.0));

      verify(cycleMetricsService).getWeekdayVsWeekendUsage(90);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getWeekdayVsWeekend_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getWeekdayVsWeekendUsage(anyInt()))
          .thenThrow(new RuntimeException("Weekday query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/trends/weekday-vs-weekend"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/trends/daily
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/trends/daily")
  class DailyTrendTests {

    @Test
    @DisplayName("200 with default 30-day window")
    void getDailyTrend_defaultDays_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkDailyTrend(30))
          .thenReturn(List.of(buildTimeSeriesDTO()));

      mockMvc
          .perform(get("/api/v1/cycle/trends/daily"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(cycleMetricsService).getNetworkDailyTrend(30);
    }

    @Test
    @DisplayName("200 with custom days parameter")
    void getDailyTrend_customDays_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkDailyTrend(14))
          .thenReturn(List.of(buildTimeSeriesDTO(), buildTimeSeriesDTO()));

      mockMvc
          .perform(get("/api/v1/cycle/trends/daily").param("days", "14"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));

      verify(cycleMetricsService).getNetworkDailyTrend(14);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getDailyTrend_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getNetworkDailyTrend(anyInt()))
          .thenThrow(new RuntimeException("Trend query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/trends/daily"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/trends/monthly
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/trends/monthly")
  class MonthlyTrendTests {

    @Test
    @DisplayName("200 with default 12-month window")
    void getMonthlyTrend_defaultMonths_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkMonthlyTrend(12))
          .thenReturn(List.of(buildTimeSeriesDTO()));

      mockMvc
          .perform(get("/api/v1/cycle/trends/monthly"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(cycleMetricsService).getNetworkMonthlyTrend(12);
    }

    @Test
    @DisplayName("200 with custom months parameter")
    void getMonthlyTrend_customMonths_returnsOk() throws Exception {
      when(cycleMetricsService.getNetworkMonthlyTrend(6)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/v1/cycle/trends/monthly").param("months", "6"))
          .andExpect(status().isOk());

      verify(cycleMetricsService).getNetworkMonthlyTrend(6);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getMonthlyTrend_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getNetworkMonthlyTrend(anyInt()))
          .thenThrow(new RuntimeException("Monthly query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/trends/monthly"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/rankings/busiest
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/rankings/busiest")
  class BusiestStationsTests {

    @Test
    @DisplayName("200 with default days=7 and limit=10")
    void getBusiestStations_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getBusiestStations(7, 10))
          .thenReturn(List.of(buildRankingDTO(1), buildRankingDTO(2)));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/busiest"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].stationId").value(1))
          .andExpect(jsonPath("$[0].avgUsageRate").value(75.0));

      verify(cycleMetricsService).getBusiestStations(7, 10);
    }

    @Test
    @DisplayName("200 with custom days and limit parameters")
    void getBusiestStations_customParams_returnsOk() throws Exception {
      when(cycleMetricsService.getBusiestStations(30, 5))
          .thenReturn(List.of(buildRankingDTO(10)));

      mockMvc
          .perform(
              get("/api/v1/cycle/rankings/busiest")
                  .param("days", "30")
                  .param("limit", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(cycleMetricsService).getBusiestStations(30, 5);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getBusiestStations_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getBusiestStations(anyInt(), anyInt()))
          .thenThrow(new RuntimeException("Rankings query failed"));

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
    @DisplayName("200 with default days=7 and limit=10")
    void getLeastUsedStations_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getLeastUsedStations(7, 10))
          .thenReturn(List.of(buildRankingDTO(50)));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/underused"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].stationId").value(50));

      verify(cycleMetricsService).getLeastUsedStations(7, 10);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getLeastUsedStations_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getLeastUsedStations(anyInt(), anyInt()))
          .thenThrow(new RuntimeException("Underused query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/rankings/underused"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/events/empty
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/events/empty")
  class EmptyEventsTests {

    @Test
    @DisplayName("200 with default days=7 and limit=50")
    void getEmptyEvents_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getEmptyEvents(7, 50))
          .thenReturn(List.of(buildEventDTO(1, "EMPTY"), buildEventDTO(2, "EMPTY")));

      mockMvc
          .perform(get("/api/v1/cycle/events/empty"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].eventType").value("EMPTY"))
          .andExpect(jsonPath("$[0].availableBikes").value(0));

      verify(cycleMetricsService).getEmptyEvents(7, 50);
    }

    @Test
    @DisplayName("200 with custom days and limit parameters")
    void getEmptyEvents_customParams_returnsOk() throws Exception {
      when(cycleMetricsService.getEmptyEvents(3, 20)).thenReturn(List.of(buildEventDTO(5, "EMPTY")));

      mockMvc
          .perform(
              get("/api/v1/cycle/events/empty")
                  .param("days", "3")
                  .param("limit", "20"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(cycleMetricsService).getEmptyEvents(3, 20);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getEmptyEvents_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getEmptyEvents(anyInt(), anyInt()))
          .thenThrow(new RuntimeException("Events query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/events/empty"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/events/full
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/events/full")
  class FullEventsTests {

    @Test
    @DisplayName("200 with default days=7 and limit=50")
    void getFullEvents_defaultParams_returnsOk() throws Exception {
      when(cycleMetricsService.getFullEvents(7, 50))
          .thenReturn(List.of(buildEventDTO(3, "FULL")));

      mockMvc
          .perform(get("/api/v1/cycle/events/full"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].eventType").value("FULL"));

      verify(cycleMetricsService).getFullEvents(7, 50);
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getFullEvents_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getFullEvents(anyInt(), anyInt()))
          .thenThrow(new RuntimeException("Full events query failed"));

      mockMvc
          .perform(get("/api/v1/cycle/events/full"))
          .andExpect(status().isInternalServerError());
    }
  }

  // =========================================================
  // GET /api/v1/cycle/network/kpi
  // =========================================================
  @Nested
  @DisplayName("GET /api/v1/cycle/network/kpi")
  class NetworkKpiTests {

    @Test
    @DisplayName("200 with full network KPI data")
    void getNetworkKpi_returnsOk() throws Exception {
      NetworkKpiDTO kpi = new NetworkKpiDTO();
      kpi.setRebalancingNeedCount(6);
      kpi.setNetworkImbalanceScore(0.25);
      kpi.setAvgHourlyTurnoverRate(1.8);
      kpi.setDailyTripsEstimate(3500L);
      kpi.setWeekdayAvgUsageRate(62.5);
      kpi.setWeekendAvgUsageRate(48.0);
      kpi.setHourlyUsageProfile(Map.of(9, 78.0, 17, 82.0));
      kpi.setDailyTrend(List.of(buildTimeSeriesDTO()));

      when(cycleMetricsService.getNetworkKpi()).thenReturn(kpi);

      mockMvc
          .perform(get("/api/v1/cycle/network/kpi"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.rebalancingNeedCount").value(6))
          .andExpect(jsonPath("$.networkImbalanceScore").value(0.25))
          .andExpect(jsonPath("$.dailyTripsEstimate").value(3500))
          .andExpect(jsonPath("$.weekdayAvgUsageRate").value(62.5))
          .andExpect(jsonPath("$.weekendAvgUsageRate").value(48.0));

      verify(cycleMetricsService).getNetworkKpi();
    }

    @Test
    @DisplayName("500 when service throws exception")
    void getNetworkKpi_serviceThrows_returns500() throws Exception {
      when(cycleMetricsService.getNetworkKpi()).thenThrow(new RuntimeException("KPI computation failed"));

      mockMvc
          .perform(get("/api/v1/cycle/network/kpi"))
          .andExpect(status().isInternalServerError());
    }
  }
}