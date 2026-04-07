package com.trinity.hermes.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemDTO {

  private String id; // Long serialised to String — matches frontend id?: string
  private String subject;
  private String body;
  private String channel; // Channel enum name as a plain string
  private String recipient;
  private boolean read;
  private String timestamp; // ISO-8601 from LocalDateTime.toString()
  private String qrCodeId;
  private String actionUrl;
  private String deletedAt; // non-null = in bin
}
