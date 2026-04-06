package com.trinity.hermes.approval.service;

import com.trinity.hermes.approval.dto.ApprovalRequestDTO;
import com.trinity.hermes.approval.dto.CreateApprovalRequestDTO;
import com.trinity.hermes.approval.dto.ReviewApprovalRequestDTO;
import com.trinity.hermes.approval.entity.ApprovalRequest;
import com.trinity.hermes.approval.repository.ApprovalRequestRepository;
import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

  private static final Set<String> REVIEWER_ROLES = Set.of("City_Manager", "Government_Admin");

  private final ApprovalRequestRepository repository;
  private final NotificationFacade notificationFacade;
  private final UserManagementService userManagementService;

  /**
   * Any indicator admin can call this to raise an approval request.
   * Notifies all City_Managers via email + in-app notification.
   */
  public ApprovalRequestDTO create(String requestedBy, CreateApprovalRequestDTO dto) {
    ApprovalRequest entity = ApprovalRequest.builder()
        .indicator(dto.getIndicator())
        .requestedBy(requestedBy)
        .payloadJson(dto.getPayloadJson())
        .summary(dto.getSummary())
        .actionUrl(dto.getActionUrl())
        .build();
    entity = repository.save(entity);
    log.info("Approval request created: id={}, indicator={}, by={}", entity.getId(), entity.getIndicator(), requestedBy);
    notifyCityManagers(entity, requestedBy);
    return toDTO(entity);
  }

  /**
   * Role-aware listing — no data leakage:
   * - City_Manager / Government_Admin: all requests across all indicators
   * - Indicator admin (e.g. Train_Admin): only their own requests
   * - Provider roles: only APPROVED requests for their indicator
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
      String indicator = indicatorFilter != null ? indicatorFilter : resolveIndicatorFromRoles(roles);
      if (indicator == null) return List.of();
      return repository.findByIndicatorOrderByCreatedAtDesc(indicator).stream()
          .filter(r -> "APPROVED".equals(r.getStatus()))
          .map(this::toDTO)
          .toList();
    }

    // Admin roles: own requests only
    if (indicatorFilter != null) {
      return repository.findByIndicatorAndRequestedByOrderByCreatedAtDesc(indicatorFilter, userId)
          .stream().map(this::toDTO).toList();
    }
    return repository.findByRequestedByOrderByCreatedAtDesc(userId)
        .stream().map(this::toDTO).toList();
  }

  /**
   * Only City_Manager / Government_Admin can approve or deny.
   * Notifies the original requester of the decision.
   */
  public ApprovalRequestDTO review(Long id, String reviewerUserId, List<String> roles,
      ReviewApprovalRequestDTO dto) {
    boolean isReviewer = roles.stream().anyMatch(REVIEWER_ROLES::contains);
    if (!isReviewer) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only City Managers may review approvals.");
    }
    if (!Set.of("APPROVED", "DENIED").contains(dto.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be APPROVED or DENIED.");
    }
    ApprovalRequest entity = repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found."));
    if (!"PENDING".equals(entity.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been reviewed.");
    }

    entity.setStatus(dto.getStatus());
    entity.setReviewedBy(reviewerUserId);
    entity.setReviewNote(dto.getReviewNote());
    entity = repository.save(entity);
    log.info("Approval request {} {} by {}", id, dto.getStatus(), reviewerUserId);
    notifyRequester(entity);
    return toDTO(entity);
  }

  // ── Private helpers ──────────────────────────────────────────────────

  private void notifyCityManagers(ApprovalRequest req, String requestedBy) {
    try {
      String subject = String.format("[%s] Approval requested by %s",
          req.getIndicator().toUpperCase(Locale.ROOT), requestedBy);
      String body = String.format(
          "A new approval request has been submitted.\n\nIndicator: %s\nRequested by: %s\n\nSummary:\n%s\n\nLog in to CityControl to review.",
          req.getIndicator(), requestedBy, req.getSummary() != null ? req.getSummary() : "—");

      userManagementService.getUsersByRole("City_Manager").forEach(u -> {
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

  private void notifyRequester(ApprovalRequest req) {
    try {
      String decision = "APPROVED".equals(req.getStatus()) ? "approved" : "denied";
      String subject = String.format("[%s] Your request has been %s",
          req.getIndicator().toUpperCase(Locale.ROOT), decision);
      String body = String.format(
          "Your approval request has been %s by %s.\n\nSummary:\n%s\n%s",
          decision, req.getReviewedBy(),
          req.getSummary() != null ? req.getSummary() : "—",
          req.getReviewNote() != null ? "\nNote: " + req.getReviewNote() : "");

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
