package com.trinity.hermes.email.facade;

import com.trinity.hermes.email.dto.EmailRequest;
import com.trinity.hermes.email.dto.EmailResponse;
import com.trinity.hermes.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Facade for Email module
 * Provides unified interface for email operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailFacade {

    private final EmailService emailService;

    /**
     * Send a single email
     */
    public EmailResponse sendEmail(EmailRequest request) {
        log.debug("EmailFacade.sendEmail: {}", request.getSubject());
        return emailService.sendEmail(request);
    }

    /**
     * Send multiple emails
     */
    public List<EmailResponse> sendBulkEmails(List<EmailRequest> requests) {
        log.debug("EmailFacade.sendBulkEmails: {} emails", requests.size());
        return emailService.sendBulkEmails(requests);
    }
}
