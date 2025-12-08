package com.trinity.hermes.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for sending notifications to manager dashboards
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardNotificationRequest {

    // Notification Type
    private NotificationType notificationType;

    // Target Users
    private List<UserRole> targetUserRoles;
    private String transportServiceId; // For filtering which transport manager receives this

    // Notification Content
    private String messageTitle;
    private String messageContent;
    private String detailedInformation;

    // Priority and Link
    private Priority priorityLevel;
    private String linkToDetails; // Link to full disruption information
    private String qrCodeUrl;

    // Disruption Context
    private Long disruptionId;
    private String affectedArea;
    private List<String> affectedRoutes;

    /**
     * Notification type enum
     */
    public enum NotificationType {
        DISRUPTION_ALERT,
        DISRUPTION_UPDATE,
        DISRUPTION_RESOLVED,
        GENERAL_ANNOUNCEMENT
    }

    /**
     * User role enum
     */
    public enum UserRole {
        TRANSPORT_MANAGER, // Manager of specific transport service
        CITY_MANAGER, // Master dashboard with access to all transports
        OPERATIONS_STAFF,
        END_USER
    }

    /**
     * Priority level enum
     */
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
