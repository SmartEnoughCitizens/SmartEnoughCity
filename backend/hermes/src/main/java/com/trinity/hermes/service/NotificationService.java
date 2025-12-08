package com.trinity.hermes.service;

import com.trinity.hermes.dto.Notification;
import com.trinity.hermes.dto.NotificationResponse;
import com.trinity.hermes.dto.NotificationType;
import com.trinity.hermes.dto.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    public NotificationResponse getUserNotifications(String userId) {
        log.info("Generating notifications for user: {}", userId);

        List<Notification> notifications = generateNotifications(userId);

        return NotificationResponse.builder()
                .userId(userId)
                .notifications(notifications)
                .totalCount(notifications.size())
                .build();
    }

    private List<Notification> generateNotifications(String userId) {
        List<Notification> notifications = new ArrayList<>();

        // Route recommendation notification
        notifications.add(Notification.builder()
                .id(UUID.randomUUID().toString())
                .type(NotificationType.ROUTE_RECOMMENDATION)
                .message("Recommended route to downtown: Via Highway 101 saves 15 minutes")
                .priority(Priority.MEDIUM)
                .timestamp(LocalDateTime.now().minusMinutes(10))
                .metadata(createRouteMetadata("Highway 101", 15, "downtown"))
                .read(false)
                .build());

        // Alert notification
        notifications.add(Notification.builder()
                .id(UUID.randomUUID().toString())
                .type(NotificationType.ALERT)
                .message("Heavy traffic detected on your usual route")
                .priority(Priority.HIGH)
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .metadata(createAlertMetadata("Main Street", "heavy_traffic"))
                .read(false)
                .build());

        // Update notification
        notifications.add(Notification.builder()
                .id(UUID.randomUUID().toString())
                .type(NotificationType.UPDATE)
                .message("Your route preferences have been updated based on recent travel patterns")
                .priority(Priority.LOW)
                .timestamp(LocalDateTime.now().minusHours(1))
                .metadata(createUpdateMetadata("route_preferences"))
                .read(true)
                .build());

        return notifications;
    }

    private Map<String, Object> createRouteMetadata(String routeName, int timeSaved, String destination) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("routeName", routeName);
        metadata.put("timeSavedMinutes", timeSaved);
        metadata.put("destination", destination);
        metadata.put("alternativeAvailable", true);
        return metadata;
    }

    private Map<String, Object> createAlertMetadata(String location, String alertType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", location);
        metadata.put("alertType", alertType);
        metadata.put("severity", "high");
        return metadata;
    }

    private Map<String, Object> createUpdateMetadata(String updateType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("updateType", updateType);
        metadata.put("source", "recommendation_engine");
        return metadata;
    }
}
