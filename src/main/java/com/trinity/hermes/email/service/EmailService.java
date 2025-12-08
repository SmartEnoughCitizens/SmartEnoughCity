package com.trinity.hermes.email.service;

import com.trinity.hermes.email.dto.EmailRequest;
import com.trinity.hermes.email.dto.EmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for sending emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /**
     * Send email
     */
    public EmailResponse sendEmail(EmailRequest request) {
        log.info("=== EMAIL SERVICE ===");
        log.info("To: {}", request.getRecipientEmails());
        log.info("Subject: {}", request.getSubject());
        log.info("Priority: {}", request.getPriority());
        log.info("Type: {}", request.getEmailType());

        // TODO: Implem 

        EmailResponse response = new EmailResponse();
        response.setSuccess(true);
        response.setMessageId(UUID.randomUUID().toString());
        response.setStatus("SENT");
        response.setSentAt(LocalDateTime.now());
        response.setRecipientCount(request.getRecipientEmails().size());

        log.info("[EMAIL] Would send to {} recipients", response.getRecipientCount());

        return response;
    }

    /**
     * Send bulk emails
     */
    public java.util.List<EmailResponse> sendBulkEmails(java.util.List<EmailRequest> requests) {
        log.info("[EMAIL] Sending {} emails", requests.size());
        return requests.stream()
                .map(this::sendEmail)
                .toList();
    }
}
