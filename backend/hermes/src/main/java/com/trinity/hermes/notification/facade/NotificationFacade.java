package com.trinity.hermes.notification.facade;

import com.trinity.hermes.notification.dto.DashboardNotificationRequest;
import com.trinity.hermes.notification.dto.QrCodeRequest;
import com.trinity.hermes.notification.dto.QrCodeResponse;
import com.trinity.hermes.notification.service.DashboardNotificationService;
import com.trinity.hermes.notification.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Facade for Notification module
 * Provides unified interface for notification operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationFacade {

    private final QrCodeService qrCodeService;
    private final DashboardNotificationService dashboardNotificationService;

    /**
     * Generate QR code for content
     */
    public QrCodeResponse generateQrCode(QrCodeRequest request) {
        log.debug("NotificationFacade.generateQrCode for disruption: {}", request.getDisruptionId());
        return qrCodeService.generateQrCode(request);
    }

    /**
     * Generate shareable link
     */
    public String generateShareableLink(Long referenceId, String title) {
        log.debug("NotificationFacade.generateShareableLink for ID: {}", referenceId);
        return qrCodeService.generateShareableLink(referenceId, title);
    }

    /**
     * Send dashboard notification to managers
     */
    public boolean sendDashboardNotification(DashboardNotificationRequest request) {
        log.debug("NotificationFacade.sendDashboardNotification: {}", request.getMessageTitle());
        return dashboardNotificationService.sendDashboardNotification(request);
    }

    /**
     * Send notifications to multiple dashboards
     */
    public int sendBulkDashboardNotifications(List<DashboardNotificationRequest> requests) {
        log.debug("NotificationFacade.sendBulkDashboardNotifications: {} notifications", requests.size());
        return dashboardNotificationService.sendBulkNotifications(requests);
    }
}
