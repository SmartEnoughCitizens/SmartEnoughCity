package com.trinity.hermes.notification.controller;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.dto.NotificationResponseDTO;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.notification.util.SseManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/notification/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {
  private final NotificationFacade notificationFacade;

  @SuppressFBWarnings(
      value = "EI2",
      justification = "Spring-injected dependency (SseManager) stored as controller field")
  private final SseManager sseManager;

  @PostMapping
  public ResponseEntity<?> receiveBackendNotification(
      @RequestBody BackendNotificationRequestDTO request) {
    log.info("Received backend notification: userId={}", request.getUserId());

    notificationFacade.handleBackendNotification(request);

    return ResponseEntity.ok(Map.of("status", "accepted"));
  }

  /** Endpoint to establish an SSE connection for streaming notifications. */
  @GetMapping("/notifications/stream")
  public SseEmitter stream(@RequestParam String userId) {
    return sseManager.register(userId);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<NotificationResponseDTO> getUserNotifications(@PathVariable String userId) {
    return ResponseEntity.ok(notificationFacade.getAll(userId));
  }

  /**
   * Mark a single notification as read. Returns 404 if the notification doesn't belong to the user.
   */
  @PatchMapping("/{userId}/{notificationId}/read")
  public ResponseEntity<?> markNotificationAsRead(
      @PathVariable String userId, @PathVariable Long notificationId) {
    boolean updated = notificationFacade.markAsRead(userId, notificationId);
    if (!updated) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(Map.of("status", "updated"));
  }
}
