package com.trinity.hermes.notification.controller;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.notification.util.SseManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/notification/v1")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationFacade notificationFacade;
  private final SseManager sseManager;

  @PostMapping
  public ResponseEntity<?> receiveBackendNotification(
      @RequestBody BackendNotificationRequestDTO request) {
    log.info("Received backend notification: userId={},", request.getUserId());

    notificationFacade.handleBackendNotification(request);

    return ResponseEntity.ok(Map.of("status", "accepted"));
  }

  /** Endpoint to establish an SSE connection for streaming notifications. */
  // TODO :  Make a seperate class for streams .. maybe?
  @GetMapping("/notifications/stream")
  public SseEmitter stream() {
    return sseManager.register();
  }

  // TODO: Remove this endpoint after DEMO
  @GetMapping("/{userId}")
  public ResponseEntity<?> getLatestNotification(@PathVariable String userId) {
    return ResponseEntity.ok(notificationFacade.getAll());
  }
}
