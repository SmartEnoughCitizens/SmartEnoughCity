package com.trinity.hermes.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for sending email notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    // Recipients
    private List<String> recipientEmails;
    private List<String> ccEmails;
    private List<String> bccEmails;

    // Email Content
    private String subject;
    private String bodyHtmlTemplate;
    private String bodyPlainText;

    // Template Variables (for dynamic content)
    private java.util.Map<String, String> templateVariables;

    // Links and Attachments
    private List<String> attachmentUrls;

    // Priority
    private EmailPriority priority;

    // Metadata
    private String emailType; // e.g., "DISRUPTION_ALERT", "ANNOUNCEMENT", etc.
    private String referenceId; // e.g., disruptionId

    /**
     * Email priority enum
     */
    public enum EmailPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}
