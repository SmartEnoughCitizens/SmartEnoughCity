package com.trinity.hermes.notification.services;

import com.trinity.hermes.notification.model.Notification;
import com.trinity.hermes.notification.services.mail.MailService;
import com.trinity.hermes.notification.services.mail.MailServiceFactory;
import com.trinity.hermes.notification.util.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

  private final MailServiceFactory mailServiceFactory;

  private final SseManager sseManager;

  /** Sends the notification via SES and returns an updated notification object. */
  public void dispatchMail(Notification notification) {

    log.info("Dispatching notification to {} via email", notification.getRecipient());

    MailService mailService = mailServiceFactory.getMailService();

    try {
      mailService.sendEmail(
          notification.getRecipient(),
          notification.getSubject(),
          notification.getBody(),
          notification.getQrCode());

      // TODO: To save in DB thin about this later
      //            return notification.toBuilder()
      //                    .status(NotificationStatus.SENT)
      //                    .sentAt(Instant.now())
      //                    .build();

    } catch (Exception e) {
      log.error(
          "Failed to send notification to {} - {}", notification.getRecipient(), e.getMessage());

      //            return notification.toBuilder()
      //                    .status(NotificationStatus.FAILED)
      //                    .sentAt(Instant.now())
      //                    .errorMessage(ex.getMessage())
      //                    .build();
    }
  }

  public void dispatchSse(Notification notification) {
    try {
      sseManager.push(notification);
      log.info("SSE pushed for notification {}", notification.getSubject());
    } catch (Exception ex) {
      log.error(
          "Failed to push SSE notification {} - {}", notification.getSubject(), ex.getMessage());
    }
  }
}
