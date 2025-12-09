package com.trinity.hermes.notification.services.mail;

import java.util.Map;

public interface MailService {
    public void sendEmail(String to,
                          String subject,
                          String htmlBody,
                          byte[] inlineImages);

}