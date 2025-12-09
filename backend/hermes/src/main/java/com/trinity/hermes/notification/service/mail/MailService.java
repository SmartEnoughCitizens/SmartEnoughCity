package com.trinity.hermes.notification.service.mail;

public interface MailService {
    public void sendEmail(String to,
                          String subject,
                          String htmlBody,
                          byte[] inlineImages);

}