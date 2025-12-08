package com.trinity.hermes.api;

import com.trinity.hermes.dto.NotificationResponse;
import com.trinity.hermes.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications/{userId}")
    public ResponseEntity<NotificationResponse> getNotifications(@PathVariable String userId) {
        log.info("Fetching notifications for user: {}", userId);
        NotificationResponse response = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(response);
    }
}
