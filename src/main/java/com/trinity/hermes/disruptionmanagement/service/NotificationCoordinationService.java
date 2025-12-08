package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.DisruptionSolution;
import com.trinity.hermes.notification.dto.DashboardNotificationRequest;
import com.trinity.hermes.notification.dto.QrCodeRequest;
import com.trinity.hermes.notification.dto.QrCodeResponse;
import com.trinity.hermes.notification.facade.NotificationFacade;
import com.trinity.hermes.email.dto.EmailRequest;
import com.trinity.hermes.email.dto.EmailResponse;
import com.trinity.hermes.email.facade.EmailFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for coordinating notifications for disruptions.
 * Orchestrates between Notification and Email modules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCoordinationService {

    private final NotificationFacade notificationFacade;
    private final EmailFacade emailFacade;

    /**
     * Send disruption solution notifications via all channels
     */
    public boolean sendDisruptionNotifications(DisruptionSolution solution) {
        log.info("=== NOTIFICATION COORDINATION STARTED ===");
        log.info("Disruption ID: {}, Severity: {}", solution.getDisruptionId(), solution.getSeverity());

        try {
            // Step 1: Generate QR code
            QrCodeResponse qrCodeResponse = generateQrCodeForDisruption(solution);
            log.info("✓ Generated QR code: {}", qrCodeResponse.getQrCodeUrl());

            // Step 2: Send dashboard notifications to managers
            sendDashboardNotifications(solution, qrCodeResponse);
            log.info("✓ Dashboard notifications sent to transport and city managers");

            // Step 3: Send email notifications
            sendEmailNotifications(solution, qrCodeResponse);
            log.info("✓ Email notifications sent");

            log.info("=== NOTIFICATION COORDINATION COMPLETED ===");
            return true;

        } catch (Exception e) {
            log.error("Error coordinating notifications for disruption {}", solution.getDisruptionId(), e);
            return false;
        }
    }

    /**
     * Generate QR code for disruption
     */
    private QrCodeResponse generateQrCodeForDisruption(DisruptionSolution solution) {
        String targetUrl = String.format("https://hermes.trinity.com/disruption/%d", solution.getDisruptionId());
        
        QrCodeRequest request = new QrCodeRequest();
        request.setDisruptionId(solution.getDisruptionId());
        request.setTargetUrl(targetUrl);
        request.setTitle("Disruption Alert: " + solution.getDisruptionType());
        request.setDescription(solution.getActionSummary());
        request.setSizePixels(300);
        
        return notificationFacade.generateQrCode(request);
    }

    /**
     * Send dashboard notifications to managers
     */
    private void sendDashboardNotifications(DisruptionSolution solution, QrCodeResponse qrCodeResponse) {
        List<DashboardNotificationRequest> notifications = new ArrayList<>();
        
        // Notification for Transport Manager
        DashboardNotificationRequest transportManagerNotif = new DashboardNotificationRequest();
        transportManagerNotif.setNotificationType(DashboardNotificationRequest.NotificationType.DISRUPTION_ALERT);
        transportManagerNotif.setTargetUserRoles(List.of(DashboardNotificationRequest.UserRole.TRANSPORT_MANAGER));
        transportManagerNotif.setTransportServiceId(determineTransportServiceId(solution));
        transportManagerNotif.setMessageTitle("DISRUPTION ALERT: " + solution.getAffectedArea());
        transportManagerNotif.setMessageContent(solution.getActionSummary());
        transportManagerNotif.setPriorityLevel(mapSeverityToPriority(solution.getSeverity()));
        transportManagerNotif.setLinkToDetails(qrCodeResponse.getShareableLink());
        transportManagerNotif.setQrCodeUrl(qrCodeResponse.getQrCodeUrl());
        transportManagerNotif.setDisruptionId(solution.getDisruptionId());
        transportManagerNotif.setAffectedArea(solution.getAffectedArea());
        notifications.add(transportManagerNotif);
        
        // Notification for City Manager
        DashboardNotificationRequest cityManagerNotif = new DashboardNotificationRequest();
        cityManagerNotif.setNotificationType(DashboardNotificationRequest.NotificationType.DISRUPTION_ALERT);
        cityManagerNotif.setTargetUserRoles(List.of(DashboardNotificationRequest.UserRole.CITY_MANAGER));
        cityManagerNotif.setMessageTitle("City-Wide Alert: " + solution.getAffectedArea());
        cityManagerNotif.setMessageContent(solution.getActionSummary());
        cityManagerNotif.setPriorityLevel(mapSeverityToPriority(solution.getSeverity()));
        cityManagerNotif.setLinkToDetails(qrCodeResponse.getShareableLink());
        cityManagerNotif.setQrCodeUrl(qrCodeResponse.getQrCodeUrl());
        cityManagerNotif.setDisruptionId(solution.getDisruptionId());
        notifications.add(cityManagerNotif);
        
        // Send all notifications
        notificationFacade.sendBulkDashboardNotifications(notifications);
    }

    /**
     * Send email notifications
     */
    private void sendEmailNotifications(DisruptionSolution solution, QrCodeResponse qrCodeResponse) {
        EmailRequest emailRequest = new EmailRequest();
        
        // Recipients (would come from user database in real implementation)
        emailRequest.setRecipientEmails(getManagerEmails(solution));
        
        // Email content
        emailRequest.setSubject("DISRUPTION ALERT: " + solution.getAffectedArea());
        emailRequest.setBodyPlainText(solution.getActionSummary());
        emailRequest.setBodyHtmlTemplate(generateHtmlTemplate(solution, qrCodeResponse));
        
        // Template variables
        Map<String, String> variables = new HashMap<>();
        variables.put("disruption_type", solution.getDisruptionType());
        variables.put("severity", solution.getSeverity());
        variables.put("affected_area", solution.getAffectedArea());
        variables.put("action_summary", solution.getActionSummary());
        variables.put("qr_code_url", qrCodeResponse.getQrCodeUrl());
        variables.put("details_link", qrCodeResponse.getShareableLink());
        emailRequest.setTemplateVariables(variables);
        
        // Priority and metadata
        emailRequest.setPriority(mapSeverityToEmailPriority(solution.getSeverity()));
        emailRequest.setEmailType("DISRUPTION_ALERT");
        emailRequest.setReferenceId(solution.getDisruptionId().toString());
        
        // Send email
        EmailResponse response = emailFacade.sendEmail(emailRequest);
        log.info("Email sent: {} - Status: {}", response.getMessageId(), response.getStatus());
    }

    /**
     * Send resolution notification when disruption is resolved
     */
    public boolean sendResolutionNotification(Long disruptionId) {
        log.info("=== RESOLUTION NOTIFICATION ===");
        log.info("Disruption ID: {} has been RESOLVED", disruptionId);
        
        // TODO: Implement actual resolution notifications
        // Would send dashboard and email notifications similar to above
        
        return true;
    }

    // Helper methods

    private String determineTransportServiceId(DisruptionSolution solution) {
        return "TRANSPORT_SERVICE_" + solution.getAffectedArea().replaceAll("\\s+", "_").toUpperCase();
    }

    private DashboardNotificationRequest.Priority mapSeverityToPriority(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> DashboardNotificationRequest.Priority.CRITICAL;
            case "HIGH" -> DashboardNotificationRequest.Priority.HIGH;
            case "MEDIUM" -> DashboardNotificationRequest.Priority.MEDIUM;
            default -> DashboardNotificationRequest.Priority.LOW;
        };
    }

    private EmailRequest.EmailPriority mapSeverityToEmailPriority(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> EmailRequest.EmailPriority.URGENT;
            case "HIGH" -> EmailRequest.EmailPriority.HIGH;
            case "MEDIUM" -> EmailRequest.EmailPriority.NORMAL;
            default -> EmailRequest.EmailPriority.LOW;
        };
    }

    private List<String> getManagerEmails(DisruptionSolution solution) {
        // Mock emails for thin slice
        return List.of(
            "transport.manager@trinity.com",
            "city.manager@trinity.com",
            "operations@trinity.com"
        );
    }

    private String generateHtmlTemplate(DisruptionSolution solution, QrCodeResponse qrCodeResponse) {
        return String.format("""
            <html>
            <body>
                <h2>Disruption Alert: %s</h2>
                <p><strong>Severity:</strong> <span style="color: red;">%s</span></p>
                <p><strong>Area:</strong> %s</p>
                <hr>
                <p>%s</p>
                <hr>
                <p><img src="%s" alt="QR Code" width="200"/></p>
                <p><a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none;">View Full Details</a></p>
            </body>
            </html>
            """, 
            solution.getDisruptionType(), 
            solution.getSeverity(), 
            solution.getAffectedArea(), 
            solution.getActionSummary(),
            qrCodeResponse.getQrCodeUrl(),
            qrCodeResponse.getShareableLink());
    }
}
