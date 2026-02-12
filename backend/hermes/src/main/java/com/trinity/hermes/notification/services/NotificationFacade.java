package com.trinity.hermes.notification.services;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.model.Notification;
import com.trinity.hermes.notification.model.User;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.util.InMemoryNotificationStore;
import com.trinity.hermes.recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.recommendation.service.RecommendationService;
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
  //private final RecommendationService recommendationService;
  private final NotificationDispatcher notificationDispatcher;
  private final InMemoryNotificationStore notificationStore;

  public void handleBackendNotification(
      BackendNotificationRequestDTO backendNotificationRequestDTO) {
    // TODO: Add code for schema validations that need to be performed via networkNT
    // TODO: Add code for user retreival
    // TODO: fix code to have facade work better
    User user = User.builder().build();
    Set<Notification> notificationSet =
        notificationService.createNotification(user, backendNotificationRequestDTO);
    for (Notification notification : notificationSet) {
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.EMAIL
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        notificationDispatcher.dispatchMail(notification);
      }
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.NOTIFICATION
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        notificationDispatcher.dispatchSse(notification);
        notificationStore.add(notification);
      }

      //recommendationService.createRecommendation(new CreateRecommendationRequest());
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

    for (Notification notification : notifications) {
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.EMAIL
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        notificationDispatcher.dispatchMail(notification);
      }
      if (Objects.nonNull(notification)
          && (notification.getChannel() == Channel.NOTIFICATION
              || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION)) {
        notificationDispatcher.dispatchSse(notification);
      }
    }
  }

  public List<Notification> getAll() {
    return notificationStore.getAll();
  }
}
