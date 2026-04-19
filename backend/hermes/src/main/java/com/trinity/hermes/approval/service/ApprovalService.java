package com.trinity.hermes.approval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.approval.dto.ApprovalRequestDTO;
import com.trinity.hermes.approval.dto.CreateApprovalRequestDTO;
import com.trinity.hermes.approval.dto.ReviewApprovalRequestDTO;
import com.trinity.hermes.approval.entity.ApprovalRequest;
import com.trinity.hermes.approval.repository.ApprovalRequestRepository;
import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.recommendation.repository.RecommendationRepository;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

  private static final Set<String> REVIEWER_ROLES = Set.of("City_Manager", "Government_Admin");

  private final ApprovalRequestRepository repository;
  private final NotificationFacade notificationFacade;
  private final UserManagementService userManagementService;
  private final ObjectMapper objectMapper;
  private final RecommendationRepository recommendationRepository;

  /**
   * Any indicator admin can call this to raise an approval request. Notifies all City_Managers via
   * email + in-app notification.
   */
  public ApprovalRequestDTO create(String requestedBy, CreateApprovalRequestDTO dto) {
    ApprovalRequest entity =
        ApprovalRequest.builder()
            .indicator(dto.getIndicator())
            .requestedBy(requestedBy)
            .payloadJson(dto.getPayloadJson())
            .summary(dto.getSummary())
            .actionUrl(dto.getActionUrl())
            .build();
    entity = repository.save(entity);
    log.info(
        "Approval request created: id={}, indicator={}, by={}",
        entity.getId(),
        entity.getIndicator(),
        requestedBy);
    notifyCityManagers(entity, requestedBy);
    return toDTO(entity);
  }

  /**
   * Creates one ApprovalRequest per DTO, then sends a single summary email to all City_Managers
   * covering all items. Use this when the user selects multiple recommendations at once.
   */
  @Transactional
  public List<ApprovalRequestDTO> createBatch(
      String requestedBy, List<CreateApprovalRequestDTO> dtos) {
    if (dtos.isEmpty()) return List.of();

    List<ApprovalRequest> saved =
        dtos.stream()
            .map(
                dto ->
                    repository.save(
                        ApprovalRequest.builder()
                            .indicator(dto.getIndicator())
                            .requestedBy(requestedBy)
                            .payloadJson(dto.getPayloadJson())
                            .summary(dto.getSummary())
                            .actionUrl(dto.getActionUrl())
                            .build()))
            .toList();

    log.info(
        "Batch approval: {} request(s) created for indicator={} by={}",
        saved.size(),
        saved.get(0).getIndicator(),
        requestedBy);

    // Mark source recommendations as submitted so they no longer appear in the Recommendations tab.
    // If DTOs carry specific row IDs (e.g. tram), mark only those rows; otherwise mark all pending
    // for the indicator (e.g. train, which always submits all at once).
    List<Integer> recIds =
        dtos.stream()
            .map(CreateApprovalRequestDTO::getRecommendationId)
            .filter(Objects::nonNull)
            .toList();
    if (!recIds.isEmpty()) {
      recommendationRepository.markSubmittedByIds(recIds);
    } else {
      recommendationRepository.markSubmittedByIndicator(dtos.get(0).getIndicator());
    }

    notifyBatchCityManagers(saved, requestedBy);
    return saved.stream().map(this::toDTO).toList();
  }

  /**
   * Role-aware listing — no data leakage: - City_Manager / Government_Admin: all requests across
   * all indicators - Indicator admin (e.g. Train_Admin): only their own requests - Provider roles:
   * only APPROVED requests for their indicator
   */
  public List<ApprovalRequestDTO> list(String userId, List<String> roles, String indicatorFilter) {
    boolean isReviewer = roles.stream().anyMatch(REVIEWER_ROLES::contains);

    if (isReviewer) {
      List<ApprovalRequest> all = repository.findAllByOrderByStatusAscCreatedAtDesc();
      return filterByIndicator(all, indicatorFilter).stream().map(this::toDTO).toList();
    }

    boolean isProvider = roles.stream().anyMatch(r -> r.endsWith("_Provider"));
    if (isProvider) {
      // Providers may only see APPROVED requests for their own indicator
      String indicator =
          indicatorFilter != null ? indicatorFilter : resolveIndicatorFromRoles(roles);
      if (indicator == null) return List.of();
      return repository.findByIndicatorOrderByCreatedAtDesc(indicator).stream()
          .filter(r -> "APPROVED".equals(r.getStatus()))
          .map(this::toDTO)
          .toList();
    }

    // Admin roles: own requests only
    if (indicatorFilter != null) {
      return repository
          .findByIndicatorAndRequestedByOrderByCreatedAtDesc(indicatorFilter, userId)
          .stream()
          .map(this::toDTO)
          .toList();
    }
    return repository.findByRequestedByOrderByCreatedAtDesc(userId).stream()
        .map(this::toDTO)
        .toList();
  }

  /**
   * Only City_Manager / Government_Admin can approve or deny. Notifies the original requester of
   * the decision.
   */
  public ApprovalRequestDTO review(
      Long id, String reviewerUserId, List<String> roles, ReviewApprovalRequestDTO dto) {
    boolean isReviewer = roles.stream().anyMatch(REVIEWER_ROLES::contains);
    if (!isReviewer) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only City Managers may review approvals.");
    }
    if (!Set.of("APPROVED", "DENIED").contains(dto.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Status must be APPROVED or DENIED.");
    }
    ApprovalRequest entity =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Approval request not found."));
    if (!"PENDING".equals(entity.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been reviewed.");
    }

    entity.setStatus(dto.getStatus());
    entity.setReviewedBy(reviewerUserId);
    entity.setReviewNote(dto.getReviewNote());
    entity = repository.save(entity);
    log.info("Approval request {} reviewed by {}", id, reviewerUserId);
    notifyRequester(entity);
    return toDTO(entity);
  }

  // ── Private helpers ──────────────────────────────────────────────────

  private void notifyBatchCityManagers(List<ApprovalRequest> requests, String requestedBy) {
    if (requests.isEmpty()) return;
    try {
      String indicator = requests.get(0).getIndicator().toUpperCase(Locale.ROOT);
      String subject =
          String.format(
              "[%s] %d approval request(s) from %s", indicator, requests.size(), requestedBy);

      StringBuilder body = new StringBuilder();
      body.append(requests.size())
          .append(" approval request(s) submitted by ")
          .append(requestedBy)
          .append(".\n\n");
      for (int i = 0; i < requests.size(); i++) {
        ApprovalRequest req = requests.get(i);
        body.append("**Request ").append(i + 1).append("**  \n");
        body.append("ID: ").append(req.getId()).append("  \n");
        if (req.getSummary() != null && !req.getSummary().isBlank()) {
          body.append(req.getSummary()).append("\n");
        }
        body.append("\n");
      }
      body.append("---\nLog in to CityControl to review.");

      String actionUrl = requests.get(0).getActionUrl();
      userManagementService
          .getUsersByRole("City_Manager")
          .forEach(
              u -> {
                BackendNotificationRequestDTO n = new BackendNotificationRequestDTO();
                n.setUserId(u.getUsername());
                n.setUserName(u.getUsername());
                n.setSubject(subject);
                n.setBody(body.toString());
                n.setChannel(Channel.EMAIL_AND_NOTIFICATION);
                n.setActionUrl(actionUrl);
                notificationFacade.handleBackendNotification(n);
              });
    } catch (Exception e) {
      log.warn("Failed to send batch notification: {}", e.getMessage());
    }
  }

  private void notifyCityManagers(ApprovalRequest req, String requestedBy) {
    try {
      String subject =
          String.format(
              "[%s] Approval requested by %s",
              req.getIndicator().toUpperCase(Locale.ROOT), requestedBy);
      String body = buildDetailedBody(req, requestedBy);

      userManagementService
          .getUsersByRole("City_Manager")
          .forEach(
              u -> {
                BackendNotificationRequestDTO n = new BackendNotificationRequestDTO();
                n.setUserId(u.getUsername());
                n.setUserName(u.getUsername());
                n.setSubject(subject);
                n.setBody(body);
                n.setChannel(Channel.EMAIL_AND_NOTIFICATION);
                n.setActionUrl(req.getActionUrl());
                notificationFacade.handleBackendNotification(n);
              });
    } catch (Exception e) {
      log.warn("Failed to notify City Managers for approval {}: {}", req.getId(), e.getMessage());
    }
  }

  private String buildDetailedBody(ApprovalRequest req, String requestedBy) {
    StringBuilder sb = new StringBuilder();
    sb.append("A new approval request has been submitted.\n\n");
    sb.append("**Indicator:** ").append(req.getIndicator().toUpperCase(Locale.ROOT)).append("  \n");
    sb.append("**Requested by:** ").append(requestedBy).append("\n");

    if (req.getPayloadJson() == null || req.getPayloadJson().isBlank()) {
      sb.append("\n").append(req.getSummary() != null ? req.getSummary() : "—");
      sb.append("\n\n---\nLog in to CityControl to review.");
      return sb.toString();
    }

    try {
      JsonNode root = objectMapper.readTree(req.getPayloadJson());

      // ── Proposed changes ──────────────────────────────────────────
      JsonNode corridors = root.path("corridors");
      if (corridors.isArray() && !corridors.isEmpty()) {
        sb.append("\n## Proposed Changes\n\n");
        for (JsonNode c : corridors) {
          sb.append("- **")
              .append(c.path("origin").asText("?"))
              .append(" → ")
              .append(c.path("destination").asText("?"))
              .append("** — +")
              .append(c.path("trainsAdded").asInt(0))
              .append(" trains/day\n");
        }
      }

      // ── Impact summary ────────────────────────────────────────────
      JsonNode impact = root.path("impact");
      if (!impact.isMissingNode()) {
        sb.append("\n## Impact Summary\n\n");
        sb.append("| | |\n|---|---|\n");
        sb.append("| Stations affected | ")
            .append(impact.path("stationsAffected").asText("—"))
            .append(" |\n");
        sb.append("| Avg demand reduction | ")
            .append(impact.path("avgDemandReduction").asText("—"))
            .append(" |\n");
        sb.append("| Most improved station | ")
            .append(impact.path("mostImprovedStation").asText("—"))
            .append(" |\n");
      }

      // ── Per-station breakdown ─────────────────────────────────────
      JsonNode stations = root.path("stationBreakdown");
      if (stations.isArray() && !stations.isEmpty()) {
        sb.append("\n## Station Breakdown\n\n");
        sb.append(
            "| Station | Demand Score | Reduction | Overcrowding Risk"
                + " | Annual Passengers | Nearby Residents |\n");
        sb.append("|---|---|---|---|---|---|\n");
        for (JsonNode st : stations) {
          String demandRange =
              st.path("demandBefore").asText("?") + " → " + st.path("demandAfter").asText("?");
          sb.append("| ")
              .append(st.path("station").asText("?"))
              .append(" | ")
              .append(demandRange)
              .append(" | ↓ ")
              .append(st.path("demandReduction").asText("?"))
              .append(" | ")
              .append(st.path("overcrowdingRisk").asText("?"))
              .append(" | ")
              .append(st.path("annualPassengers").asText("?"))
              .append(" | ")
              .append(st.path("nearbyResidents").asText("?"))
              .append(" |\n");
        }
        sb.append(
            "\n_Annual passengers and nearby residents are percentile ranks"
                + " relative to all Dublin train stops._\n");
      }

    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.warn("Could not parse payloadJson for approval {}: {}", req.getId(), e.getMessage());
      sb.append("\n").append(req.getSummary() != null ? req.getSummary() : "—");
    }

    sb.append("\n---\nLog in to CityControl to review.");
    return sb.toString();
  }

  private void notifyRequester(ApprovalRequest req) {
    try {
      String decision = "APPROVED".equals(req.getStatus()) ? "approved" : "denied";
      String subject =
          String.format(
              "[%s] Your request has been %s",
              req.getIndicator().toUpperCase(Locale.ROOT), decision);
      String reviewNote =
          req.getReviewNote() != null
              ? (System.lineSeparator() + "Note: " + req.getReviewNote())
              : "";
      String body =
          String.format(
              "Your approval request has been %s by %s.%n%nSummary:%n%s%s",
              decision,
              req.getReviewedBy(),
              req.getSummary() != null ? req.getSummary() : "—",
              reviewNote);

      BackendNotificationRequestDTO n = new BackendNotificationRequestDTO();
      n.setUserId(req.getRequestedBy());
      n.setUserName(req.getRequestedBy());
      n.setSubject(subject);
      n.setBody(body);
      n.setChannel(Channel.EMAIL_AND_NOTIFICATION);
      n.setActionUrl(req.getActionUrl());
      notificationFacade.handleBackendNotification(n);
    } catch (Exception e) {
      log.warn("Failed to notify requester for approval {}: {}", req.getId(), e.getMessage());
    }
  }

  private List<ApprovalRequest> filterByIndicator(List<ApprovalRequest> list, String indicator) {
    if (indicator == null || indicator.isBlank()) return list;
    return list.stream().filter(r -> indicator.equalsIgnoreCase(r.getIndicator())).toList();
  }

  private String resolveIndicatorFromRoles(List<String> roles) {
    for (String role : roles) {
      if (role.endsWith("_Provider")) return role.replace("_Provider", "").toLowerCase(Locale.ROOT);
    }
    return null;
  }

  private ApprovalRequestDTO toDTO(ApprovalRequest e) {
    return ApprovalRequestDTO.builder()
        .id(e.getId())
        .indicator(e.getIndicator())
        .requestedBy(e.getRequestedBy())
        .status(e.getStatus())
        .payloadJson(e.getPayloadJson())
        .summary(e.getSummary())
        .reviewedBy(e.getReviewedBy())
        .reviewNote(e.getReviewNote())
        .actionUrl(e.getActionUrl())
        .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
        .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
        .build();
  }
}
