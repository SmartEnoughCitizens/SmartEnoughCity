package com.trinity.hermes.notification.services.mail;

public interface MailService {
  public void sendEmail(String to, String subject, String htmlBody, byte[] inlineImages);
}
