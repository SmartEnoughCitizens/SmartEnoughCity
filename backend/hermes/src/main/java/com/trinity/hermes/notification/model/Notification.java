package com.trinity.hermes.notification.model;

import com.trinity.hermes.notification.model.enums.Channel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Notification {
  private String recipient;
  private String subject;
  private String body;
  private Channel channel;
  private byte[] qrCode;

    public byte[] getQrCode() {
        return qrCode == null ? null : qrCode.clone();
    }

    public static class NotificationBuilder {
        public NotificationBuilder qrCode(byte[] qrCode) {
            this.qrCode = qrCode == null ? null : qrCode.clone();
            return this;
        }
    }
  //    private String templateKey;
  //    private Map<String, Object> data; // original data if you want
}
