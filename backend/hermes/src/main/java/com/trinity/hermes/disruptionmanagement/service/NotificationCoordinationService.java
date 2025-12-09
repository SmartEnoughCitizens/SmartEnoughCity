package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.DisruptionSolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for coordinating notifications for disruptions.
 * Simplified stub implementation for thin slice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCoordinationService {

    /**
     * Send disruption solution notifications via all channels
     */
    public boolean sendDisruptionNotifications(DisruptionSolution solution) {
        log.info("=== NOTIFICATION COORDINATION STARTED (STUB) ===");
        log.info("Disruption ID: {}, Severity: {}", solution.getDisruptionId(), solution.getSeverity());

        try {
            // Mock QR Code Generation
            String qrCodeUrl = "https://mock.qr.code/" + UUID.randomUUID();
            log.info("✓ Generated Mock QR code: {}", qrCodeUrl);

            // Mock Dashboard Notifications
            log.info("✓ [MOCK] Dashboard notifications sent to transport and city managers about {}",
                    solution.getAffectedArea());

            // Mock Email Notifications
            log.info("✓ [MOCK] Email notifications sent via stub service");

            log.info("=== NOTIFICATION COORDINATION COMPLETED ===");
            return true;

        } catch (Exception e) {
            log.error("Error coordinating notifications for disruption {}", solution.getDisruptionId(), e);
            return false;
        }
    }

    /**
     * Send resolution notification when disruption is resolved
     */
    public boolean sendResolutionNotification(Long disruptionId) {
        log.info("=== RESOLUTION NOTIFICATION (STUB) ===");
        log.info("Disruption ID: {} has been RESOLVED", disruptionId);
        log.info("✓ [MOCK] Resolution notifications sent to all channels");

        return true;
    }
}
