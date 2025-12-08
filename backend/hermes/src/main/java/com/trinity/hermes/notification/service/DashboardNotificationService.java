package com.trinity.hermes.notification.service;

import com.trinity.hermes.notification.dto.DashboardNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending dashboard notifications to managers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardNotificationService {

    /**
     * Send notification to dashboard
     */
    public boolean sendDashboardNotification(DashboardNotificationRequest request) {
        log.info("=== DASHBOARD NOTIFICATION ===");
        log.info("Type: {}", request.getNotificationType());
        log.info("Target Roles: {}", request.getTargetUserRoles());
        log.info("Title: {}", request.getMessageTitle());
        log.info("Priority: {}", request.getPriorityLevel());

        // TODO: Implement actual dashboard notification (WebSocket, SSE, etc.)
        // For thin slice: just log

        request.getTargetUserRoles().forEach(role -> {
            log.info("[DASHBOARD -> {}] {}", role, request.getMessageTitle());
        });

        return true;
    }

    /**
     * Send bulk notifications to multiple dashboards
     */
    public int sendBulkNotifications(java.util.List<DashboardNotificationRequest> requests) {
        log.info("[DASHBOARD] Sending {} notifications", requests.size());
        requests.forEach(this::sendDashboardNotification);
        return requests.size();
    }
}
