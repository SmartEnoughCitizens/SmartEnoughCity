package com.trinity.hermes.indicators.cycle.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.cycle.dto.*;
import com.trinity.hermes.indicators.cycle.service.CycleMetricsService;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/cycle")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Validated
public class CycleMetricsController {

  private final CycleMetricsService cycleMetricsService;
  private final UserManagementService userManagementService;

  // -------------------------------------------------------------------------
  // Live Station Data
  // -------------------------------------------------------------------------

  @GetMapping("/stations/live")
  public ResponseEntity<List<StationLiveDTO>> getLiveStations() {
    log.info("GET /api/v1/cycle/stations/live");
    try {
      return ResponseEntity.ok(cycleMetricsService.getLiveStations());
    } catch (Exception e) {
      log.error("Error fetching live stations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/network/summary")
  public ResponseEntity<NetworkSummaryDTO> getNetworkSummary() {
    log.info("GET /api/v1/cycle/network/summary");
    try {
      return ResponseEntity.ok(cycleMetricsService.getNetworkSummary());
    } catch (Exception e) {
      log.error("Error fetching network summary: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/regions")
  public ResponseEntity<List<RegionMetricsDTO>> getRegionMetrics() {
    log.info("GET /api/v1/cycle/regions");
    try {
      return ResponseEntity.ok(cycleMetricsService.getRegionMetrics());
    } catch (Exception e) {
      log.error("Error fetching region metrics: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Station Rankings
  // -------------------------------------------------------------------------

  @GetMapping("/rankings/busiest")
  public ResponseEntity<List<StationRankingDTO>> getBusiestStations(
      @RequestParam(defaultValue = "10") @Min(1) @Max(200) int limit) {
    log.info("GET /api/v1/cycle/rankings/busiest limit={}", LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getBusiestStations(limit));
    } catch (Exception e) {
      log.error("Error fetching busiest stations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/rankings/underused")
  public ResponseEntity<List<StationRankingDTO>> getLeastUsedStations(
      @RequestParam(defaultValue = "10") @Min(1) @Max(200) int limit) {
    log.info("GET /api/v1/cycle/rankings/underused limit={}", LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getLeastUsedStations(limit));
    } catch (Exception e) {
      log.error("Error fetching least used stations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Rebalancing Suggestions
  // -------------------------------------------------------------------------

  @GetMapping("/network/rebalancing")
  public ResponseEntity<List<RebalanceSuggestionDTO>> getRebalancingSuggestions(
      @RequestParam(defaultValue = "30") @Min(1) @Max(200) int limit) {
    log.info("GET /api/v1/cycle/network/rebalancing limit={}", LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getRebalancingSuggestions(limit));
    } catch (Exception e) {
      log.error("Error fetching rebalancing suggestions: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Demand Analysis
  // -------------------------------------------------------------------------

  @GetMapping("/demand/network-hourly")
  public ResponseEntity<List<HourlyNetworkProfileDTO>> getNetworkHourlyProfile(
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
    log.info("GET /api/v1/cycle/demand/network-hourly days={}", LogSanitizer.sanitizeLog(days));
    try {
      return ResponseEntity.ok(cycleMetricsService.getNetworkHourlyProfile(days));
    } catch (Exception e) {
      log.error("Error fetching network hourly profile: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/demand/classification")
  public ResponseEntity<List<StationClassificationDTO>> getStationClassification(
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
    log.info("GET /api/v1/cycle/demand/classification days={}", LogSanitizer.sanitizeLog(days));
    try {
      return ResponseEntity.ok(cycleMetricsService.getStationClassification(days));
    } catch (Exception e) {
      log.error("Error fetching station classification: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/demand/od-pairs")
  public ResponseEntity<List<StationODPairDTO>> getODPairs(
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
    log.info(
        "GET /api/v1/cycle/demand/od-pairs days={} limit={}",
        LogSanitizer.sanitizeLog(days),
        LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getODPairs(days, limit));
    } catch (Exception e) {
      log.error("Error fetching OD pairs: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Risk Scores (ML)
  // -------------------------------------------------------------------------

  @GetMapping("/risk-scores")
  public ResponseEntity<List<StationRiskScoreDTO>> getRiskScores() {
    log.info("GET /api/v1/cycle/risk-scores");
    try {
      return ResponseEntity.ok(cycleMetricsService.getRiskScores());
    } catch (Exception e) {
      log.error("Error fetching station risk scores: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/demand/station-hourly")
  public ResponseEntity<List<StationHourlyUsageDTO>> getStationHourlyUsage(
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
      @RequestParam(defaultValue = "30") @Min(1) @Max(200) int limit) {
    log.info(
        "GET /api/v1/cycle/demand/station-hourly days={} limit={}",
        LogSanitizer.sanitizeLog(days),
        LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getStationHourlyUsage(days, limit));
    } catch (Exception e) {
      log.error("Error fetching station hourly usage: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Coverage Gap Analysis
  // -------------------------------------------------------------------------

  @GetMapping("/coverage-gaps")
  public ResponseEntity<List<CoverageGapDTO>> getCoverageGaps() {
    log.info("GET /api/v1/cycle/coverage-gaps");
    try {
      return ResponseEntity.ok(cycleMetricsService.getCoverageGaps());
    } catch (Exception e) {
      log.error("Error fetching coverage gaps: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PostMapping("/coverage-gaps/proposals")
  public ResponseEntity<Void> submitStationProposal(
      @RequestBody StationProposalDTO proposal, @AuthenticationPrincipal Jwt jwt) {
    log.info(
        "POST /api/v1/cycle/coverage-gaps/proposals — {} proposed stations, {} impacted areas",
        proposal.getProposedStations() != null ? proposal.getProposedStations().size() : 0,
        proposal.getTotalImprovedAreas());
    try {
      String submitterRole = resolveSubmitterRole(jwt);
      cycleMetricsService.submitStationProposal(proposal, submitterRole);
      cycleMetricsService.notifyProposalRecipients(proposal, submitterRole);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (Exception e) {
      log.error("Error submitting station proposal: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Returns the highest-priority cycle-related role the caller holds. */
  private String resolveSubmitterRole(Jwt jwt) {
    if (jwt == null) return "unknown";
    var realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess == null) return "unknown";
    @SuppressWarnings("unchecked")
    var roles = (java.util.List<String>) realmAccess.get("roles");
    if (roles == null) return "unknown";
    if (roles.contains("City_Manager")) return "City_Manager";
    if (roles.contains("Cycle_Admin")) return "Cycle_Admin";
    return "unknown";
  }

  // -------------------------------------------------------------------------
  // Proposal Review
  // -------------------------------------------------------------------------

  @GetMapping("/coverage-gaps/proposals/accepted")
  public ResponseEntity<List<StationProposalSummaryDTO>> getAcceptedProposals() {
    log.info("GET /api/v1/cycle/coverage-gaps/proposals/accepted");
    try {
      return ResponseEntity.ok(cycleMetricsService.getAcceptedProposals());
    } catch (Exception e) {
      log.error("Error fetching accepted proposals: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/coverage-gaps/proposals")
  public ResponseEntity<List<StationProposalSummaryDTO>> getPendingProposals(
      @AuthenticationPrincipal Jwt jwt) {
    log.info("GET /api/v1/cycle/coverage-gaps/proposals");
    try {
      String requesterRole = resolveSubmitterRole(jwt);
      return ResponseEntity.ok(cycleMetricsService.getPendingProposals(requesterRole));
    } catch (Exception e) {
      log.error("Error fetching pending proposals: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PatchMapping("/coverage-gaps/proposals/{id}/review")
  public ResponseEntity<Void> reviewProposal(
      @PathVariable Long id,
      @RequestBody ProposalReviewDTO review,
      @AuthenticationPrincipal Jwt jwt) {
    log.info(
        "PATCH /api/v1/cycle/coverage-gaps/proposals/{}/review action={}", id, review.getAction());
    if (!"ACCEPTED".equals(review.getAction()) && !"REJECTED".equals(review.getAction())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "action must be ACCEPTED or REJECTED");
    }
    try {
      String reviewerRole = resolveSubmitterRole(jwt);
      cycleMetricsService.reviewProposal(id, review, reviewerRole, reviewerRole);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("Error reviewing proposal id={}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PatchMapping("/coverage-gaps/proposals/{id}/implementation-status")
  @PreAuthorize("hasRole('Cycle_Admin')")
  public ResponseEntity<Void> updateImplementationStatus(
      @PathVariable Long id,
      @RequestBody ProposalImplementationStatusDTO body,
      @AuthenticationPrincipal Jwt jwt) {
    log.info(
        "PATCH /api/v1/cycle/coverage-gaps/proposals/{}/implementation-status status={}",
        id,
        body.getStatus());
    var allowed = java.util.Set.of("PLANNED", "IN_PROGRESS", "COMPLETED");
    if (!allowed.contains(body.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "status must be PLANNED, IN_PROGRESS or COMPLETED");
    }
    try {
      String updaterUsername = resolveSubmitterRole(jwt);
      cycleMetricsService.updateImplementationStatus(id, body.getStatus(), updaterUsername);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("Error updating implementation status for proposal id={}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PatchMapping("/coverage-gaps/{electoralDivision}/process")
  public ResponseEntity<Void> processGap(@PathVariable String electoralDivision) {
    log.info(
        "PATCH /api/v1/cycle/coverage-gaps/{}/process",
        LogSanitizer.sanitizeLog(electoralDivision));
    try {
      boolean updated = cycleMetricsService.processGap(electoralDivision);
      return updated
          ? ResponseEntity.noContent().build()
          : ResponseEntity.status(HttpStatus.CONFLICT).build();
    } catch (Exception e) {
      log.error("Error processing coverage gap: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Debug / Test
  // -------------------------------------------------------------------------

  @GetMapping("/debug/whoami")
  public ResponseEntity<java.util.Map<String, Object>> whoami(
      @AuthenticationPrincipal Jwt jwt) {
    java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
    if (jwt == null) {
      result.put("jwt", "null");
      return ResponseEntity.ok(result);
    }
    String sid = jwt.getClaimAsString("sid");
    result.put("jwtClaims", jwt.getClaims());
    result.put("sid", sid);
    result.put("resolvedUsername", userManagementService.getUsernameBySid(sid));
    return ResponseEntity.ok(result);
  }
}
