package com.trinity.hermes.notification.controller;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.dto.BroadcastNotificationRequestDTO;
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
    log.info(" Received backend notification: userId={}", request.getUserId());

    notificationFacade.handleBackendNotification(request);

    return ResponseEntity.ok(Map.of("status", "accepted"));
  }

  /** Internal endpoint for broadcasting a notification to all providers of a given indicator. */
  @PostMapping("/broadcast")
  public ResponseEntity<?> broadcastByIndicator(
      @RequestBody BroadcastNotificationRequestDTO request) {
    log.info("Received broadcast request for indicator={}", request.getDataIndicator());
    notificationFacade.broadcastByIndicator(request);
    return ResponseEntity.ok(Map.of("status", "broadcast accepted"));
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

  @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }

  @PatchMapping("/{userId}/{notificationId}/read")
  public ResponseEntity<?> toggleRead(
      @PathVariable String userId,
      @PathVariable Long notificationId,
      @RequestParam(defaultValue = "true") boolean read) {
    boolean updated =
        read
            ? notificationFacade.markAsRead(userId, notificationId)
            : notificationFacade.markAsUnread(userId, notificationId);
    if (!updated) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(Map.of("status", "updated"));
  }

  /** Soft-delete (move to bin). */
  @DeleteMapping("/{userId}/{notificationId}")
  public ResponseEntity<?> softDelete(
      @PathVariable String userId, @PathVariable Long notificationId) {
    boolean deleted = notificationFacade.softDelete(userId, notificationId);
    if (!deleted) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(Map.of("status", "deleted"));
  }

  /** Restore from bin. */
  @PatchMapping("/{userId}/{notificationId}/restore")
  public ResponseEntity<?> restore(@PathVariable String userId, @PathVariable Long notificationId) {
    boolean restored = notificationFacade.restore(userId, notificationId);
    if (!restored) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(Map.of("status", "restored"));
  }

  /** Get bin (soft-deleted) notifications. */
  @GetMapping("/{userId}/bin")
  public ResponseEntity<NotificationResponseDTO> getBin(@PathVariable String userId) {
    return ResponseEntity.ok(notificationFacade.getBin(userId));
  }
}
