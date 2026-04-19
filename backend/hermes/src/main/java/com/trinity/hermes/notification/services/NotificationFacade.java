package com.trinity.hermes.notification.services;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.dto.BroadcastNotificationRequestDTO;
import com.trinity.hermes.notification.dto.NotificationItemDTO;
import com.trinity.hermes.notification.dto.NotificationResponseDTO;
import com.trinity.hermes.notification.entity.NotificationEntity;
import com.trinity.hermes.notification.model.Notification;
import com.trinity.hermes.notification.model.User;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.repository.NotificationRepository;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(
    value = "EI2",
    justification = "Facade stores Spring-injected dependencies; not an encapsulation risk")
public class NotificationFacade {

  private final NotificationService notificationService;
  // private final RecommendationService recommendationService;
  private final NotificationDispatcher notificationDispatcher;
  private final UserManagementService userManagementService;
  private final NotificationRepository notificationRepository;

  public void handleBackendNotification(
      BackendNotificationRequestDTO backendNotificationRequestDTO) {
    // TODO: Add code for schema validations that need to be performed via networkNT
    // TODO: fix code to have facade work better
    String userId = backendNotificationRequestDTO.getUserId();
    String email = null;
    try {
      email = userManagementService.getUserEmail(userId);
    } catch (Exception e) {
      log.warn("Could not resolve email for userId {}, falling back to default", userId);
    }
    User user = User.builder().id(userId).email(email).build();
    Set<Notification> notificationSet =
        notificationService.createNotification(user, backendNotificationRequestDTO);
    if (notificationSet == null) return;
    for (Notification notification : notificationSet) {
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.EMAIL
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        notificationDispatcher.dispatchMail(notification);
      }
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.NOTIFICATION
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        notificationDispatcher.dispatchSse(userId, notification);
        notificationRepository.save(
            NotificationEntity.builder()
                .userId(userId)
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .channel(notification.getChannel())
                .isRead(false)
                .qrCodeId(backendNotificationRequestDTO.getQrid())
                .actionUrl(backendNotificationRequestDTO.getActionUrl())
                .build());
        log.info("Persisted notification for userId={}", userId);
      }
    }
  }

  private static final Map<String, String> INDICATOR_ROLE_MAP =
      Map.of(
          "bus", "Bus_Provider",
          "train", "Train_Provider",
          "tram", "Tram_Provider",
          "cycle", "Cycle_Provider");

  public void broadcastByIndicator(BroadcastNotificationRequestDTO request) {
    String indicator = request.getDataIndicator();
    String roleName = INDICATOR_ROLE_MAP.get(indicator);
    if (roleName == null) {
      throw new IllegalArgumentException("No provider role mapped for indicator: " + indicator);
    }
    // Merge indicator-role users and City_Manager users, deduplicating by userId
    java.util.Map<String, org.keycloak.representations.idm.UserRepresentation> uniqueUsers =
        new java.util.LinkedHashMap<>();
    userManagementService.getUsersByRole(roleName).forEach(u -> uniqueUsers.put(u.getId(), u));
    userManagementService
        .getUsersByRole("City_Manager")
        .forEach(u -> uniqueUsers.put(u.getId(), u));
    List<org.keycloak.representations.idm.UserRepresentation> users =
        new java.util.ArrayList<>(uniqueUsers.values());
    log.info(
        "Broadcasting indicator={} to {} users (role={} + City_Manager)",
        indicator,
        users.size(),
        roleName);
    for (org.keycloak.representations.idm.UserRepresentation user : users) {
      BackendNotificationRequestDTO dto = new BackendNotificationRequestDTO();
      dto.setUserId(user.getUsername());
      dto.setUserName(user.getUsername());
      dto.setQrid(request.getQrid());
      dto.setDataIndicator(indicator);
      dto.setRecommendation(request.getRecommendation());
      dto.setSimulation(request.getSimulation());
      dto.setSubject(request.getSubject());
      dto.setBody(request.getBody());
      dto.setMetadata(request.getMetadata());
      dto.setPriority(request.getPriority());
      dto.setChannel(request.getChannel());
      handleBackendNotification(dto);
    }
  }

  public void sendDisruptionNotification(
      com.trinity.hermes.disruptionmanagement.dto.DisruptionSolution solution) {
    log.info("Sending disruption notification for ID: {}", solution.getDisruptionId());

    // Prepare payload for notification service
    // Needs QR_ID (mapped to "qrid") for QR code generation
    java.util.Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("qrid", solution.getDisruptionId());
    payload.put("subject", "Disruption Alert: " + solution.getAffectedArea());
    // Construct comprehensive body with recommendations
    StringBuilder bodyBuilder = new StringBuilder();
    bodyBuilder.append(
        solution.getActionSummary() != null
            ? solution.getActionSummary()
            : solution.getDescription());

    if (solution.getPrimaryRecommendation() != null) {
      bodyBuilder.append("\n\nRecommendation: ").append(solution.getPrimaryRecommendation());
    }
    if (solution.getAlternativeRoutes() != null && !solution.getAlternativeRoutes().isEmpty()) {
      bodyBuilder
          .append("\n\nAlternatives:\n")
          .append(String.join("\n", solution.getAlternativeRoutes()));
    }

    payload.put("body", bodyBuilder.toString());

    // Also keep structured data if needed
    if (solution.getPrimaryRecommendation() != null)
      payload.put("primaryRecommendation", solution.getPrimaryRecommendation());
    if (solution.getAlternativeRoutes() != null && !solution.getAlternativeRoutes().isEmpty()) {
      payload.put("alternativeRoutes", String.join("\n", solution.getAlternativeRoutes()));
    }

    // Add other metadata if needed
    payload.put("severity", solution.getSeverity());
    payload.put("disruptionType", solution.getDisruptionType());

    // Email — City_Manager only
    try {
      userManagementService
          .getUsersByRole("City_Manager")
          .forEach(
              cm -> {
                String email = cm.getEmail();
                if (email == null || email.isBlank()) return;
                User user = User.builder().id(cm.getId()).email(email).build();
                Set<Notification> notifications =
                    notificationService.createNotification(user, payload);
                if (notifications == null) return;
                for (Notification notification : notifications) {
                  if (Objects.nonNull(notification)
                      && (notification.getChannel() == Channel.EMAIL
                          || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
                    notificationDispatcher.dispatchMail(notification);
                  }
                }
              });
    } catch (Exception e) {
      log.warn("Failed to send disruption email to City_Manager: {}", e.getMessage());
    }
  }

  public NotificationResponseDTO getAll(String userId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<NotificationEntity> entityPage =
        notificationRepository.findByUserIdAndDeletedAtIsNull(userId, pageable);
    long unreadCount = notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
    return NotificationResponseDTO.builder()
        .userId(userId)
        .notifications(entityPage.getContent().stream().map(this::toItemDTO).toList())
        .totalCount(unreadCount)
        .totalItems(entityPage.getTotalElements())
        .page(page)
        .pageSize(size)
        .build();
  }

  public NotificationResponseDTO getBin(String userId) {
    List<NotificationEntity> entities =
        notificationRepository.findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId);
    return NotificationResponseDTO.builder()
        .userId(userId)
        .notifications(entities.stream().map(this::toItemDTO).toList())
        .totalCount(entities.size())
        .build();
  }

  @jakarta.transaction.Transactional
  public int markAllAsRead(String userId) {
    return notificationRepository.markAllAsReadByUserId(userId);
  }

  public boolean markAsRead(String userId, Long notificationId) {
    return setReadState(userId, notificationId, true);
  }

  public boolean markAsUnread(String userId, Long notificationId) {
    return setReadState(userId, notificationId, false);
  }

  private boolean setReadState(String userId, Long notificationId, boolean read) {
    return notificationRepository
        .findByIdAndUserId(notificationId, userId)
        .map(
            entity -> {
              entity.setRead(read);
              notificationRepository.save(entity);
              return true;
            })
        .orElse(false);
  }

  public boolean softDelete(String userId, Long notificationId) {
    return notificationRepository
        .findByIdAndUserId(notificationId, userId)
        .map(
            entity -> {
              entity.setDeletedAt(LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")));
              notificationRepository.save(entity);
              log.info("Soft-deleted notification {} for userId={}", notificationId, userId);
              return true;
            })
        .orElse(false);
  }

  public boolean restore(String userId, Long notificationId) {
    return notificationRepository
        .findByIdAndUserId(notificationId, userId)
        .map(
            entity -> {
              entity.setDeletedAt(null);
              notificationRepository.save(entity);
              return true;
            })
        .orElse(false);
  }

  @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
  @jakarta.transaction.Transactional
  public void purgeExpiredBin() {
    LocalDateTime cutoff = LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")).minusDays(30);
    notificationRepository.hardDeleteExpiredBinEntries(cutoff);
    log.info("Purged bin notifications older than 30 days");
  }

  private NotificationItemDTO toItemDTO(NotificationEntity entity) {
    return NotificationItemDTO.builder()
        .id(String.valueOf(entity.getId()))
        .subject(entity.getSubject())
        .body(entity.getBody())
        .channel(entity.getChannel() != null ? entity.getChannel().name() : null)
        .recipient(entity.getRecipient())
        .read(entity.isRead())
        .timestamp(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
        .qrCodeId(entity.getQrCodeId())
        .actionUrl(entity.getActionUrl())
        .deletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toString() : null)
        .build();
  }
}
