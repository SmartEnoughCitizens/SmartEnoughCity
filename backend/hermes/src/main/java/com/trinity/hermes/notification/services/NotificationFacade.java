package com.trinity.hermes.notification.services;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.dto.NotificationItemDTO;
import com.trinity.hermes.notification.dto.NotificationResponseDTO;
import com.trinity.hermes.notification.entity.NotificationEntity;
import com.trinity.hermes.notification.model.Notification;
import com.trinity.hermes.notification.model.User;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.repository.NotificationRepository;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .build());
        log.info("Persisted notification for userId={}", userId);
      }
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

    // Use a dummy user or appropriate recipient logic
    User user = User.builder().build();

    Set<Notification> notifications = notificationService.createNotification(user, payload);
    if (notifications == null) return;

    List<String> userGroups =
        solution.getAffectedUserGroups() != null ? solution.getAffectedUserGroups() : List.of();

    for (Notification notification : notifications) {
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.EMAIL
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        notificationDispatcher.dispatchMail(notification);
      }
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.NOTIFICATION
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        for (String userId : userGroups) {
          notificationDispatcher.dispatchSse(userId, notification);
        }
      }
    }
  }

  public NotificationResponseDTO getAll(String userId) {
    List<NotificationEntity> entities =
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
    return NotificationResponseDTO.builder()
        .userId(userId)
        .notifications(entities.stream().map(this::toItemDTO).toList())
        .totalCount(unreadCount)
        .build();
  }

  public boolean markAsRead(String userId, Long notificationId) {
    return notificationRepository
        .findByIdAndUserId(notificationId, userId)
        .map(
            entity -> {
              entity.setRead(true);
              notificationRepository.save(entity);
              log.info("Marked notification {} as read for userId={}", notificationId, userId);
              return true;
            })
        .orElse(false);
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
        .build();
  }
}
