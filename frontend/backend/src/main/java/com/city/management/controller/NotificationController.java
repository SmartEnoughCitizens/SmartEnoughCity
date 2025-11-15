package com.city.management.controller;

import com.city.management.model.Notification;
import com.city.management.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private MockDataService mockDataService;

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(mockDataService.getAllNotifications());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Optional<Notification> notification = mockDataService.markNotificationAsRead(id);

        if (notification.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(notification.get());
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        mockDataService.markAllNotificationsAsRead();
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
