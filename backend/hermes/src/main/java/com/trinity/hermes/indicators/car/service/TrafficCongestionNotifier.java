package com.trinity.hermes.indicators.car.service;

import com.trinity.hermes.indicators.car.entity.TrafficRecommendation;
import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficCongestionNotifier {

  private static final String ACTION_URL = "/dashboard?view=car";

  private final NotificationFacade notificationFacade;
  private final UserManagementService userManagementService;

  public void notifyForRecommendation(TrafficRecommendation rec) {
    String subject =
        String.format(
            "Diversion plan for Site %d — %s congestion",
            rec.getSiteId(), capitalize(rec.getCongestionLevel()));
    String body = buildBody(rec);
    dispatch(subject, body);
    log.info("Diversion notification sent to City_Manager users for site {}.", rec.getSiteId());
  }

  private void dispatch(String subject, String body) {
    userManagementService
        .getUsersByRole("City_Manager")
        .forEach(
            user -> {
              BackendNotificationRequestDTO dto = new BackendNotificationRequestDTO();
              dto.setUserId(user.getUsername());
              dto.setUserName(user.getUsername());
              dto.setSubject(subject);
              dto.setBody(body);
              dto.setChannel(Channel.NOTIFICATION);
              dto.setActionUrl(ACTION_URL);
              notificationFacade.handleBackendNotification(dto);
            });
  }

  private String buildBody(TrafficRecommendation r) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "**Site %d** — %s congestion (confidence: %d%%)%n%n%s%n%n",
            r.getSiteId(),
            capitalize(r.getCongestionLevel()),
            Math.round(r.getConfidenceScore() * 100),
            r.getRecommendedAction()));
    for (TrafficRecommendation.AlternativeRoute route : r.getAlternativeRoutes()) {
      sb.append(
          String.format(
              "- **%s**: saves ~%d min, %.1f km%n",
              route.getLabel(), route.getEstimatedTimeSavingsMinutes(), route.getDistanceKm()));
    }
    return sb.toString().strip();
  }

  private String capitalize(String value) {
    if (value == null || value.isBlank()) return "";
    return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
  }
}
