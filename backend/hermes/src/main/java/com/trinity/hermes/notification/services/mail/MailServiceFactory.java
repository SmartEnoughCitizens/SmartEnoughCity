package com.trinity.hermes.notification.services.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailServiceFactory {

    private final SesMailService sesMailService;

    /**
     * Basic function that returns SES implementation of the MailService. Later Can be changed to include other implementations.
     * @return Mailservice
     */
    @Bean
    public MailService getMailService() {
        return sesMailService;
    }
}