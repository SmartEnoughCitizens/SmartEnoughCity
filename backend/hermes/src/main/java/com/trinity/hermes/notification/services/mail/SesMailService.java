package com.trinity.hermes.notification.services.mail;

import jakarta.activation.DataHandler;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SesMailService implements MailService {

  private final SesV2Client sesV2Client;

  @Value("${mail.from}")
  private String fromAddress;

  public void sendEmail(String to, String subject, String htmlBody, byte[] qrCodeBytes) {

    try {
      MimeMessage message = buildMime(to, subject, htmlBody, qrCodeBytes);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      message.writeTo(out);

      SendEmailRequest req =
          SendEmailRequest.builder()
              .fromEmailAddress(fromAddress)
              .destination(Destination.builder().toAddresses(to).build())
              .content(
                  EmailContent.builder()
                      .raw(
                          RawMessage.builder()
                              .data(SdkBytes.fromByteArray(out.toByteArray()))
                              .build())
                      .build())
              .build();

      sesV2Client.sendEmail(req);
      log.info("SES email sent to {}", to);

    } catch (Exception e) {
      throw new RuntimeException("SES send failed: " + e.getMessage(), e);
    }
  }

  private MimeMessage buildMime(String to, String subject, String htmlBody, byte[] qrCodeBytes)
      throws Exception {

    MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
    msg.setFrom(new InternetAddress(fromAddress));
    msg.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
    msg.setSubject(subject, "UTF-8");

    MimeMultipart related = new MimeMultipart("related");

    // 1) HTML part
    MimeBodyPart html = new MimeBodyPart();
    html.setContent(htmlBodyWithPlaceholder(htmlBody, qrCodeBytes), "text/html; charset=UTF-8");
    related.addBodyPart(html);

    // 2) Add inline QR only if present
    if (Objects.nonNull(qrCodeBytes)) {
      related.addBodyPart(inlineQrPart("qr", qrCodeBytes));
    }

    msg.setContent(related);
    return msg;
  }

  private String htmlBodyWithPlaceholder(String html, byte[] qrBytes) {
    if (Objects.nonNull(qrBytes)) return html;
    return html.replace("{{qr}}", "<img src=\"cid:qr\" alt=\"QR Code\"/>");
  }

  private MimeBodyPart inlineQrPart(String cid, byte[] bytes) throws Exception {
    MimeBodyPart part = new MimeBodyPart();
    part.setDisposition(MimeBodyPart.INLINE);
    part.setHeader("Content-ID", "<" + cid + ">");
    part.setDataHandler(new DataHandler(new ByteArrayDataSource(bytes, "image/png")));
    return part;
  }
}
