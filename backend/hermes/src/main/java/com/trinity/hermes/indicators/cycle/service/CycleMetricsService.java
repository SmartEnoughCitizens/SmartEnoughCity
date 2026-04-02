package com.trinity.hermes.indicators.cycle.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.cycle.dto.CoverageGapDTO;
import com.trinity.hermes.indicators.cycle.dto.HourlyNetworkProfileDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.ProposalReviewDTO;
import com.trinity.hermes.indicators.cycle.dto.RebalanceSuggestionDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.StationClassificationDTO;
import com.trinity.hermes.indicators.cycle.dto.StationHourlyUsageDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationProposalDTO;
import com.trinity.hermes.indicators.cycle.dto.StationProposalSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRiskScoreDTO;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesSnapshotRepository;
import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CycleMetricsService {

  private final DublinBikesSnapshotRepository snapshotRepository;
  private final JdbcTemplate jdbcTemplate;
  private final NotificationFacade notificationFacade;
  private final UserManagementService userManagementService;

  // -------------------------------------------------------------------------
  // Proposals table initialisation
  // -------------------------------------------------------------------------

  @PostConstruct
  public void initProposalsTable() {
    jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS backend.cycle_station_proposals ("
            + "id                  BIGSERIAL PRIMARY KEY, "
            + "submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(), "
            + "submitted_by        VARCHAR(100), "
            + "submitted_by_role   VARCHAR(50), "
            + "station_count       INTEGER NOT NULL, "
            + "improved_area_count INTEGER NOT NULL, "
            + "stations_json       TEXT NOT NULL, "
            + "impacts_json        TEXT NOT NULL, "
            + "notes               TEXT, "
            + "status              VARCHAR(30) NOT NULL DEFAULT 'PENDING'"
            + ")");
    jdbcTemplate.execute(
        "ALTER TABLE backend.cycle_station_proposals "
            + "ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ");
    jdbcTemplate.execute(
        "ALTER TABLE backend.cycle_station_proposals "
            + "ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(100)");
    jdbcTemplate.execute(
        "ALTER TABLE backend.cycle_station_proposals "
            + "ADD COLUMN IF NOT EXISTS review_reason TEXT");
  }

  // -------------------------------------------------------------------------
  // Live Station Data
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationLiveDTO> getLiveStations() {
    log.debug("Fetching live station data for all stations");
    List<Object[]> rows = snapshotRepository.findLatestSnapshotPerStation();
    return rows.stream().map(this::mapToStationLiveDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public NetworkSummaryDTO getNetworkSummary() {
    log.debug("Fetching network summary");
    Object[] row = firstRow(snapshotRepository.findNetworkSummary());
    if (row == null) {
      return new NetworkSummaryDTO();
    }

    int totalStations = toIntOrDefault(row[0], 0);
    int activeStations = toIntOrDefault(row[1], 0);
    int totalBikes = toIntOrDefault(row[2], 0);
    int totalDocks = toIntOrDefault(row[3], 0);
    int disabledBikes = toIntOrDefault(row[4], 0);
    int disabledDocks = toIntOrDefault(row[5], 0);
    int emptyStations = toIntOrDefault(row[6], 0);
    int fullStations = toIntOrDefault(row[7], 0);
    double avgFullness = toDoubleOrDefault(row[8], 0.0);
    Instant latestTimestamp = toInstant(row[9]);

    NetworkSummaryDTO dto = new NetworkSummaryDTO();
    dto.setTotalStations(totalStations);
    dto.setActiveStations(activeStations);
    dto.setTotalBikesAvailable(totalBikes);
    dto.setTotalDocksAvailable(totalDocks);
    dto.setTotalDisabledBikes(disabledBikes);
    dto.setTotalDisabledDocks(disabledDocks);
    dto.setEmptyStations(emptyStations);
    dto.setFullStations(fullStations);
    dto.setAvgNetworkFullnessPct(avgFullness);
    dto.setRebalancingNeedCount(emptyStations + fullStations);
    dto.setDataAsOf(latestTimestamp);
    return dto;
  }

  @Transactional(readOnly = true)
  public List<RegionMetricsDTO> getRegionMetrics() {
    log.debug("Fetching region-level metrics");
    List<Object[]> rows = snapshotRepository.findRegionMetrics();
    return rows.stream()
        .map(
            row -> {
              RegionMetricsDTO dto = new RegionMetricsDTO();
              dto.setRegionId((String) row[0]);
              dto.setStationCount(toLong(row[1]));
              dto.setTotalCapacity(toLong(row[2]));
              dto.setAvgUsageRate(toDouble(row[3]));
              dto.setAvgAvailableBikes(toDouble(row[4]));
              dto.setAvgAvailableDocks(toDouble(row[5]));
              dto.setEmptyStations(toLong(row[6]));
              dto.setFullStations(toLong(row[7]));
              return dto;
            })
        .collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Station Rankings
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationRankingDTO> getBusiestStations(int limit) {
    List<Object[]> rows = snapshotRepository.findBusiestStations(limit);
    return rows.stream().map(this::mapToRankingDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationRankingDTO> getLeastUsedStations(int limit) {
    List<Object[]> rows = snapshotRepository.findLeastUsedStations(limit);
    return rows.stream().map(this::mapToRankingDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Rebalancing Suggestions
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<RebalanceSuggestionDTO> getRebalancingSuggestions(int limit) {
    log.debug("Fetching rebalancing suggestions, limit {}", limit);
    List<Object[]> rows = snapshotRepository.findRebalancingSuggestions(limit);
    return rows.stream().map(this::mapToRebalanceSuggestionDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Demand Analysis

  @Transactional(readOnly = true)
  public List<HourlyNetworkProfileDTO> getNetworkHourlyProfile(int days) {
    log.debug("Fetching network hourly profile for last {} days", days);
    List<Object[]> rows = snapshotRepository.findNetworkHourlyProfile(days);
    return rows.stream().map(this::mapToHourlyNetworkProfileDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationClassificationDTO> getStationClassification(int days) {
    log.debug("Fetching station classification for last {} days", days);
    List<Object[]> rows = snapshotRepository.findStationClassification(days);
    return rows.stream().map(this::mapToStationClassificationDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationODPairDTO> getODPairs(int days, int limit) {
    log.debug("Fetching OD pairs for last {} days, limit {}", days, limit);
    List<Object[]> rows = snapshotRepository.findODPairs(days, limit);
    return rows.stream().map(this::mapToODPairDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationHourlyUsageDTO> getStationHourlyUsage(int days, int stationLimit) {
    log.debug(
        "Fetching station hourly usage for last {} days, top {} stations", days, stationLimit);
    List<Object[]> rows = snapshotRepository.findStationHourlyUsage(days, stationLimit);
    return rows.stream().map(this::mapToStationHourlyUsageDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Risk Scores
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationRiskScoreDTO> getRiskScores() {
    log.debug("Fetching ML risk scores for all stations");
    List<Object[]> rows = snapshotRepository.findStationRiskScores();
    return rows.stream().map(this::mapToStationRiskScoreDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Coverage Gap Analysis
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<CoverageGapDTO> getCoverageGaps() {
    log.debug("Fetching coverage gap analysis");
    List<Object[]> rows = snapshotRepository.findCoverageGaps();
    return rows.stream().map(this::mapToCoverageGapDTO).collect(Collectors.toList());
  }

  @Transactional
  public boolean processGap(String electoralDivision) {
    log.info("Marking coverage gap as processed: {}", LogSanitizer.sanitizeLog(electoralDivision));
    int updated =
        jdbcTemplate.update(
            """
        UPDATE backend.cycle_coverage_gaps
           SET processed_for_implementation = TRUE,
               processed_at = NOW()
         WHERE electoral_division = ? AND processed_for_implementation = FALSE
        """,
            electoralDivision);
    return updated > 0;
  }

  // -------------------------------------------------------------------------
  // Proposal Retrieval
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationProposalSummaryDTO> getPendingProposals(String requesterRole) {
    // City_Manager reviews proposals from Cycle_Admin, and vice versa
    String submitterRole =
        "City_Manager".equals(requesterRole)
            ? "Cycle_Admin"
            : "Cycle_Admin".equals(requesterRole) ? "City_Manager" : null;

    if (submitterRole == null) {
      return List.of();
    }

    return jdbcTemplate.query(
        """
        SELECT id, submitted_at, submitted_by, submitted_by_role,
               station_count, improved_area_count, status, notes,
               stations_json, impacts_json
          FROM backend.cycle_station_proposals
         WHERE status = 'PENDING'
           AND submitted_by_role = ?
         ORDER BY submitted_at DESC
        """,
        (rs, rn) -> {
          StationProposalSummaryDTO dto = new StationProposalSummaryDTO();
          dto.setId(rs.getLong("id"));
          dto.setSubmittedAt(rs.getTimestamp("submitted_at").toInstant().toString());
          dto.setSubmittedBy(rs.getString("submitted_by"));
          dto.setSubmittedByRole(rs.getString("submitted_by_role"));
          dto.setStationCount(rs.getInt("station_count"));
          dto.setImprovedAreaCount(rs.getInt("improved_area_count"));
          dto.setStatus(rs.getString("status"));
          dto.setNotes(rs.getString("notes"));
          dto.setStationsJson(rs.getString("stations_json"));
          dto.setImpactsJson(rs.getString("impacts_json"));
          return dto;
        },
        submitterRole);
  }

  // -------------------------------------------------------------------------
  // Proposal Review
  // -------------------------------------------------------------------------

  @Transactional
  public void reviewProposal(Long id, ProposalReviewDTO review, String reviewerUsername) {
    int updated =
        jdbcTemplate.update(
            """
        UPDATE backend.cycle_station_proposals
           SET status        = ?,
               reviewed_at   = NOW(),
               reviewed_by   = ?,
               review_reason = ?
         WHERE id = ? AND status = 'PENDING'
        """,
            review.getAction(),
            reviewerUsername,
            review.getReason(),
            id);

    if (updated == 0) {
      log.warn("Proposal id={} not found or already reviewed", LogSanitizer.sanitizeLog(id));
      return;
    }

    // Notify the original submitter
    String submittedBy =
        jdbcTemplate.queryForObject(
            "SELECT submitted_by FROM backend.cycle_station_proposals WHERE id = ?",
            String.class,
            id);

    if (submittedBy == null) return;

    String subject =
        "ACCEPTED".equals(review.getAction())
            ? "Your station proposal was accepted"
            : "Your station proposal was rejected";
    String body =
        "ACCEPTED".equals(review.getAction())
            ? "Your proposed station(s) have been accepted by " + reviewerUsername + "."
            : "Your proposed station(s) were rejected by "
                + reviewerUsername
                + ". Reason: "
                + review.getReason();

    try {
      BackendNotificationRequestDTO notification = new BackendNotificationRequestDTO();
      notification.setUserId(submittedBy);
      notification.setSubject(subject);
      notification.setBody(body);
      notification.setChannel(Channel.NOTIFICATION);
      notificationFacade.handleBackendNotification(notification);
      log.info("Review notification sent to submitter username={}", submittedBy);
    } catch (Exception e) {
      log.error("Failed to notify submitter username={}: {}", submittedBy, e.getMessage(), e);
    }
  }

  // -------------------------------------------------------------------------
  // Proposal Submission
  // -------------------------------------------------------------------------

  @Transactional
  public void submitStationProposal(StationProposalDTO proposal, String submitterRole) {
    log.info(
        "Station proposal received from role={}: {} stations, {} areas impacted",
        submitterRole,
        proposal.getProposedStations() != null ? proposal.getProposedStations().size() : 0,
        proposal.getTotalImprovedAreas());

    String stationsJson =
        proposal.getProposedStations() == null
            ? "[]"
            : proposal.getProposedStations().stream()
                .map(
                    s ->
                        String.format(
                            "{\"lat\":%.6f,\"lon\":%.6f}", s.getLatitude(), s.getLongitude()))
                .collect(Collectors.joining(",", "[", "]"));

    String impactsJson =
        proposal.getImpactedAreas() == null
            ? "[]"
            : proposal.getImpactedAreas().stream()
                .map(
                    a ->
                        String.format(
                            "{\"area\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"distM\":%.1f}",
                            a.getElectoralDivision(),
                            a.getFromCategory(),
                            a.getToCategory(),
                            a.getSimulatedDistanceM()))
                .collect(Collectors.joining(",", "[", "]"));

    jdbcTemplate.update(
        """
        INSERT INTO backend.cycle_station_proposals
            (submitted_by, submitted_by_role, station_count, improved_area_count, stations_json, impacts_json, notes)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        proposal.getSubmittedBy(),
        submitterRole,
        proposal.getProposedStations() != null ? proposal.getProposedStations().size() : 0,
        proposal.getTotalImprovedAreas(),
        stationsJson,
        impactsJson,
        proposal.getNotes());

    log.info(
        "Station proposal saved — submitted_by={} submitted_by_role={}",
        proposal.getSubmittedBy(),
        submitterRole);
  }

  /**
   * Sends notifications to target role after a proposal is submitted. Deliberately
   * NOT @Transactional so Keycloak/SSE failures don't affect the saved proposal.
   */
  public void notifyProposalRecipients(StationProposalDTO proposal, String submitterRole) {
    String targetRole =
        "City_Manager".equals(submitterRole)
            ? "Cycle_Admin"
            : "Cycle_Admin".equals(submitterRole) ? "City_Manager" : null;

    if (targetRole == null) {
      log.warn("Cannot determine notification target for submitterRole='{}'", submitterRole);
      return;
    }

    String submittedBy =
        proposal.getSubmittedBy() != null ? proposal.getSubmittedBy() : submitterRole;
    String subject = "New Station Proposal — " + submittedBy;
    String body =
        proposal.getProposedStations().size()
            + " proposed station(s) would improve "
            + proposal.getTotalImprovedAreas()
            + " area(s) in Dublin. "
            + "Submitted by: "
            + submittedBy
            + ". "
            + "Please review the proposal in the Cycle Coverage map.";

    try {
      var recipients = userManagementService.getUsersByRole(targetRole);
      if (recipients.isEmpty()) {
        log.warn("No users found for role={} — no notifications sent", targetRole);
        return;
      }

      recipients.forEach(
          user -> {
            try {
              BackendNotificationRequestDTO notification = new BackendNotificationRequestDTO();
              notification.setUserId(user.getUsername());
              notification.setSubject(subject);
              notification.setBody(body);
              notification.setChannel(Channel.NOTIFICATION);
              notificationFacade.handleBackendNotification(notification);
              log.info("Station proposal notification sent to username={}", user.getUsername());
            } catch (Exception e) {
              log.error("Failed to notify username={}: {}", user.getUsername(), e.getMessage(), e);
            }
          });
    } catch (Exception e) {
      log.error("Failed to fetch users for role={}: {}", targetRole, e.getMessage(), e);
    }
  }

  // -------------------------------------------------------------------------
  // Mapping helpers
  // -------------------------------------------------------------------------

  private StationLiveDTO mapToStationLiveDTO(Object[] row) {
    Integer stationId = toInt(row[0]);
    Integer capacity = toInt(row[6]);
    Integer availableBikes = toInt(row[8]);
    Integer availableDocks = toInt(row[9]);

    double bikeAvailabilityPct = 0.0;
    double dockAvailabilityPct = 0.0;
    if (capacity != null && capacity > 0) {
      if (availableBikes != null) bikeAvailabilityPct = (double) availableBikes / capacity * 100.0;
      if (availableDocks != null) dockAvailabilityPct = (double) availableDocks / capacity * 100.0;
    }

    String statusColor =
        bikeAvailabilityPct < 20 ? "RED" : bikeAvailabilityPct < 40 ? "YELLOW" : "GREEN";

    StationLiveDTO dto = new StationLiveDTO();
    dto.setStationId(stationId);
    dto.setName((String) row[1]);
    dto.setShortName((String) row[2]);
    dto.setAddress((String) row[3]);
    dto.setLatitude(row[4] != null ? new BigDecimal(row[4].toString()) : null);
    dto.setLongitude(row[5] != null ? new BigDecimal(row[5].toString()) : null);
    dto.setCapacity(capacity);
    dto.setRegionId((String) row[7]);
    dto.setAvailableBikes(availableBikes);
    dto.setAvailableDocks(availableDocks);
    dto.setDisabledBikes(row[10] != null ? toInt(row[10]) : null);
    dto.setDisabledDocks(row[11] != null ? toInt(row[11]) : null);
    dto.setIsInstalled((Boolean) row[12]);
    dto.setIsRenting((Boolean) row[13]);
    dto.setIsReturning((Boolean) row[14]);
    dto.setLastReported(toInstant(row[15]));
    dto.setSnapshotTimestamp(toInstant(row[16]));
    dto.setBikeAvailabilityPct(bikeAvailabilityPct);
    dto.setDockAvailabilityPct(dockAvailabilityPct);
    dto.setStatusColor(statusColor);
    dto.setIsEmpty(availableBikes != null && availableBikes == 0);
    dto.setIsFull(availableDocks != null && availableDocks == 0);
    return dto;
  }

  private StationRankingDTO mapToRankingDTO(Object[] row) {
    StationRankingDTO dto = new StationRankingDTO();
    dto.setStationId(toInt(row[0]));
    dto.setName((String) row[1]);
    dto.setAvgUsageRate(toDouble(row[2]));
    return dto;
  }

  private RebalanceSuggestionDTO mapToRebalanceSuggestionDTO(Object[] row) {
    RebalanceSuggestionDTO dto = new RebalanceSuggestionDTO();
    dto.setSourceStationId(toInt(row[0]));
    dto.setSourceName((String) row[1]);
    dto.setSourceLat(row[2] != null ? new BigDecimal(row[2].toString()) : null);
    dto.setSourceLon(row[3] != null ? new BigDecimal(row[3].toString()) : null);
    dto.setSourceBikes(toInt(row[4]));
    dto.setTargetStationId(toInt(row[5]));
    dto.setTargetName((String) row[6]);
    dto.setTargetLat(row[7] != null ? new BigDecimal(row[7].toString()) : null);
    dto.setTargetLon(row[8] != null ? new BigDecimal(row[8].toString()) : null);
    dto.setTargetCapacity(toInt(row[9]));
    dto.setDistanceKm(toDouble(row[10]));
    return dto;
  }

  private HourlyNetworkProfileDTO mapToHourlyNetworkProfileDTO(Object[] row) {
    HourlyNetworkProfileDTO dto = new HourlyNetworkProfileDTO();
    dto.setHourOfDay(toIntOrDefault(row[0], 0));
    dto.setAvgTurnover(toDoubleOrDefault(row[1], 0.0));
    dto.setStationCount(toLong(row[2]));
    return dto;
  }

  private StationClassificationDTO mapToStationClassificationDTO(Object[] row) {
    StationClassificationDTO dto = new StationClassificationDTO();
    dto.setStationId(toIntOrDefault(row[0], 0));
    dto.setName((String) row[1]);
    dto.setPeakHour(toIntOrDefault(row[2], 0));
    dto.setPeakUsage(toDoubleOrDefault(row[3], 0.0));
    dto.setClassification((String) row[4]);
    return dto;
  }

  private StationHourlyUsageDTO mapToStationHourlyUsageDTO(Object[] row) {
    StationHourlyUsageDTO dto = new StationHourlyUsageDTO();
    dto.setStationId(toIntOrDefault(row[0], 0));
    dto.setName((String) row[1]);
    dto.setHourOfDay(toIntOrDefault(row[2], 0));
    dto.setAvgTurnover(toDoubleOrDefault(row[3], 0.0));
    return dto;
  }

  private CoverageGapDTO mapToCoverageGapDTO(Object[] row) {
    CoverageGapDTO dto = new CoverageGapDTO();
    dto.setElectoralDivision((String) row[0]);
    dto.setFlatApartmentCount(toIntOrDefault(row[1], 0));
    dto.setHouseBungalowCount(toIntOrDefault(row[2], 0));
    dto.setTotalDwellings(toIntOrDefault(row[3], 0));
    dto.setCentroidLat(toDoubleOrDefault(row[4], 0.0));
    dto.setCentroidLon(toDoubleOrDefault(row[5], 0.0));
    dto.setMinDistanceM(row[6] != null ? toDouble(row[6]) : null);
    dto.setCoverageCategory((String) row[7]);
    dto.setPriorityScore(toIntOrDefault(row[8], 0));
    dto.setComputedAt(toInstant(row[9]));
    dto.setProcessedForImplementation(row[10] != null && (Boolean) row[10]);
    dto.setProcessedAt(row[11] != null ? toInstant(row[11]) : null);
    dto.setGeomGeoJson(row[12] != null ? (String) row[12] : null);
    return dto;
  }

  private StationRiskScoreDTO mapToStationRiskScoreDTO(Object[] row) {
    StationRiskScoreDTO dto = new StationRiskScoreDTO();
    dto.setStationId(toIntOrDefault(row[0], 0));
    dto.setName((String) row[1]);
    dto.setLatitude(row[2] != null ? new BigDecimal(row[2].toString()) : null);
    dto.setLongitude(row[3] != null ? new BigDecimal(row[3].toString()) : null);
    dto.setEmptyRisk2h(toDoubleOrDefault(row[4], 0.0));
    dto.setFullRisk2h(toDoubleOrDefault(row[5], 0.0));
    dto.setScoredAt(toInstant(row[6]));
    dto.setModelTrainedAt(toInstant(row[7]));
    return dto;
  }

  private StationODPairDTO mapToODPairDTO(Object[] row) {
    StationODPairDTO dto = new StationODPairDTO();
    dto.setOriginStationId(toIntOrDefault(row[0], 0));
    dto.setOriginName((String) row[1]);
    dto.setOriginLat(row[2] != null ? new BigDecimal(row[2].toString()) : null);
    dto.setOriginLon(row[3] != null ? new BigDecimal(row[3].toString()) : null);
    dto.setDestStationId(toIntOrDefault(row[4], 0));
    dto.setDestName((String) row[5]);
    dto.setDestLat(row[6] != null ? new BigDecimal(row[6].toString()) : null);
    dto.setDestLon(row[7] != null ? new BigDecimal(row[7].toString()) : null);
    dto.setEstimatedTrips(toIntOrDefault(row[8], 0));
    dto.setDistanceKm(toDoubleOrDefault(row[9], 0.0));
    return dto;
  }

  // -------------------------------------------------------------------------
  // Type conversion utilities
  // -------------------------------------------------------------------------

  private Integer toInt(Object value) {
    if (value == null) return null;
    if (value instanceof Integer i) return i;
    if (value instanceof Long l) return l.intValue();
    if (value instanceof Number n) return n.intValue();
    return Integer.parseInt(value.toString());
  }

  private Long toLong(Object value) {
    if (value == null) return null;
    if (value instanceof Long l) return l;
    if (value instanceof Number n) return n.longValue();
    return Long.parseLong(value.toString());
  }

  private Double toDouble(Object value) {
    if (value == null) return null;
    if (value instanceof Double d) return d;
    if (value instanceof Number n) return n.doubleValue();
    return Double.parseDouble(value.toString());
  }

  private Instant toInstant(Object value) {
    if (value == null) return null;
    if (value instanceof Instant i) return i;
    if (value instanceof Timestamp ts) return ts.toInstant();
    if (value instanceof java.util.Date d) return d.toInstant();
    return Instant.parse(value.toString());
  }

  private int toIntOrDefault(Object value, int defaultValue) {
    Integer result = toInt(value);
    return result != null ? result : defaultValue;
  }

  private double toDoubleOrDefault(Object value, double defaultValue) {
    Double result = toDouble(value);
    return result != null ? result : defaultValue;
  }

  private Object[] firstRow(List<Object[]> rows) {
    return (rows == null || rows.isEmpty()) ? null : rows.get(0);
  }
}
