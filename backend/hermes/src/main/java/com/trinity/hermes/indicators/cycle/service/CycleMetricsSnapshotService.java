package com.trinity.hermes.indicators.cycle.service;

import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RebalanceSuggestionDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.entity.CycleNetworkKpiSnapshot;
import com.trinity.hermes.indicators.cycle.entity.CycleOdFlowSnapshot;
import com.trinity.hermes.indicators.cycle.entity.CycleRebalancingLog;
import com.trinity.hermes.indicators.cycle.entity.CycleStationDailyMetrics;
import com.trinity.hermes.indicators.cycle.repository.CycleNetworkKpiSnapshotRepository;
import com.trinity.hermes.indicators.cycle.repository.CycleOdFlowSnapshotRepository;
import com.trinity.hermes.indicators.cycle.repository.CycleRebalancingLogRepository;
import com.trinity.hermes.indicators.cycle.repository.CycleStationDailyMetricsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically persists computed cycle metrics to the backend schema so the inference /
 * recommendation engine can consume them as historical inputs without re-running expensive queries.
 *
 * <p>Schedule summary:
 *
 * <ul>
 *   <li>Every hour → {@code cycle_network_kpi_snapshot}
 *   <li>Daily at 02:00 → {@code cycle_station_daily_metrics}
 *   <li>Weekly (Monday 03:00) → {@code cycle_od_flow_snapshot}
 *   <li>Every hour (offset) → {@code cycle_rebalancing_log}
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CycleMetricsSnapshotService {

  private static final int OD_LIMIT = 100;
  private static final int RANKING_LIMIT = 50;
  private static final int REBALANCING_LIMIT = 20;

  private final CycleMetricsService metricsService;
  private final CycleNetworkKpiSnapshotRepository kpiSnapshotRepo;
  private final CycleStationDailyMetricsRepository stationDailyRepo;
  private final CycleOdFlowSnapshotRepository odFlowRepo;
  private final CycleRebalancingLogRepository rebalancingLogRepo;

  // ── Network KPI snapshot (every hour) ─────────────────────────────────────

  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void snapshotNetworkKpi() {
    Instant hourKey = Instant.now().truncatedTo(ChronoUnit.HOURS);
    if (kpiSnapshotRepo.findBySnapshotAt(hourKey).isPresent()) {
      log.debug("Network KPI snapshot already exists for {}, skipping", hourKey);
      return;
    }

    try {
      NetworkSummaryDTO summary = metricsService.getNetworkSummary();

      CycleNetworkKpiSnapshot snapshot = new CycleNetworkKpiSnapshot();
      snapshot.setSnapshotAt(hourKey);
      snapshot.setTotalStations(summary.getTotalStations());
      snapshot.setTotalBikesAvailable(summary.getTotalBikesAvailable());
      snapshot.setTotalDocksAvailable(summary.getTotalDocksAvailable());
      snapshot.setEmptyStations(summary.getEmptyStations());
      snapshot.setFullStations(summary.getFullStations());
      snapshot.setAvgNetworkFullnessPct(summary.getAvgNetworkFullnessPct());
      snapshot.setRebalancingNeedCount(summary.getRebalancingNeedCount());

      kpiSnapshotRepo.save(snapshot);
      log.info("Saved network KPI snapshot for {}", hourKey);
    } catch (Exception e) {
      log.error("Failed to save network KPI snapshot for {}", hourKey, e);
    }
  }

  // ── Station daily metrics (daily at 02:00) ─────────────────────────────────

  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void snapshotStationDailyMetrics() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    try {
      List<StationRankingDTO> busiest = metricsService.getBusiestStations(RANKING_LIMIT);
      List<StationRankingDTO> underused = metricsService.getLeastUsedStations(RANKING_LIMIT);
      List<StationLiveDTO> liveStations = metricsService.getLiveStations();

      // Build live station lookup
      Map<Integer, StationLiveDTO> liveMap = new HashMap<>();
      for (StationLiveDTO live : liveStations) {
        liveMap.put(live.getStationId(), live);
      }

      // Merge: include all ranked stations
      Map<Integer, CycleStationDailyMetrics> metricsMap = new HashMap<>();

      for (int rank = 0; rank < busiest.size(); rank++) {
        StationRankingDTO dto = busiest.get(rank);
        CycleStationDailyMetrics m = getOrCreate(metricsMap, dto.getStationId(), dto.getName(), today);
        m.setBusiestRank(rank + 1);
        m.setAvgUsageRatePct(dto.getAvgUsageRate());
      }

      for (int rank = 0; rank < underused.size(); rank++) {
        StationRankingDTO dto = underused.get(rank);
        CycleStationDailyMetrics m = getOrCreate(metricsMap, dto.getStationId(), dto.getName(), today);
        m.setUnderusedRank(rank + 1);
        if (m.getAvgUsageRatePct() == null) {
          m.setAvgUsageRatePct(dto.getAvgUsageRate());
        }
      }

      // Populate live availability for all stations in the map
      for (CycleStationDailyMetrics m : metricsMap.values()) {
        StationLiveDTO live = liveMap.get(m.getStationId());
        if (live != null) {
          m.setBikeAvailabilityPct(live.getBikeAvailabilityPct());
          m.setDockAvailabilityPct(live.getDockAvailabilityPct());
          m.setStatusColor(live.getStatusColor());
        }
      }

      // Upsert: skip stations already recorded for today
      List<CycleStationDailyMetrics> toSave = new ArrayList<>();
      for (CycleStationDailyMetrics m : metricsMap.values()) {
        if (!stationDailyRepo.existsByStationIdAndMetricDate(m.getStationId(), today)) {
          toSave.add(m);
        }
      }

      stationDailyRepo.saveAll(toSave);
      log.info("Saved {} station daily metric rows for {}", toSave.size(), today);
    } catch (Exception e) {
      log.error("Failed to save station daily metrics for {}", today, e);
    }
  }

  // ── OD flow snapshot (weekly — every Monday at 03:00) ─────────────────────

  @Scheduled(cron = "0 0 3 * * MON")
  @Transactional
  public void snapshotOdFlow() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    if (odFlowRepo.existsBySnapshotDate(today)) {
      log.debug("OD flow snapshot already exists for {}, skipping", today);
      return;
    }

    try {
      List<StationODPairDTO> pairs = metricsService.getODPairs(30, OD_LIMIT);
      List<CycleOdFlowSnapshot> rows = new ArrayList<>(pairs.size());
      for (StationODPairDTO pair : pairs) {
        CycleOdFlowSnapshot row = new CycleOdFlowSnapshot();
        row.setSnapshotDate(today);
        row.setOriginStationId(pair.getOriginStationId());
        row.setOriginName(pair.getOriginName());
        row.setDestStationId(pair.getDestStationId());
        row.setDestName(pair.getDestName());
        row.setEstimatedTrips((long) pair.getEstimatedTrips());
        rows.add(row);
      }
      odFlowRepo.saveAll(rows);
      log.info("Saved {} OD flow rows for week starting {}", rows.size(), today);
    } catch (Exception e) {
      log.error("Failed to save OD flow snapshot for {}", today, e);
    }
  }

  // ── Rebalancing log (every hour, 30 min offset) ────────────────────────────

  @Scheduled(cron = "0 30 * * * *")
  @Transactional
  public void logRebalancingSuggestions() {
    try {
      List<RebalanceSuggestionDTO> suggestions =
          metricsService.getRebalancingSuggestions(REBALANCING_LIMIT);
      if (suggestions.isEmpty()) {
        log.debug("No rebalancing suggestions to log");
        return;
      }

      Instant now = Instant.now();
      List<CycleRebalancingLog> rows = new ArrayList<>(suggestions.size());
      for (int i = 0; i < suggestions.size(); i++) {
        RebalanceSuggestionDTO dto = suggestions.get(i);
        CycleRebalancingLog row = new CycleRebalancingLog();
        row.setLoggedAt(now);
        row.setPriorityRank(i + 1);
        row.setSourceStationId(dto.getSourceStationId());
        row.setSourceName(dto.getSourceName());
        row.setSourceBikes(dto.getSourceBikes());
        row.setTargetStationId(dto.getTargetStationId());
        row.setTargetName(dto.getTargetName());
        row.setTargetCapacity(dto.getTargetCapacity());
        row.setDistanceKm(dto.getDistanceKm());
        rows.add(row);
      }
      rebalancingLogRepo.saveAll(rows);
      log.info("Logged {} rebalancing suggestions at {}", rows.size(), now);
    } catch (Exception e) {
      log.error("Failed to log rebalancing suggestions", e);
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private CycleStationDailyMetrics getOrCreate(
      Map<Integer, CycleStationDailyMetrics> map, Integer stationId, String name, LocalDate date) {
    return map.computeIfAbsent(
        stationId,
        id -> {
          CycleStationDailyMetrics m = new CycleStationDailyMetrics();
          m.setStationId(id);
          m.setStationName(name);
          m.setMetricDate(date);
          return m;
        });
  }
}
