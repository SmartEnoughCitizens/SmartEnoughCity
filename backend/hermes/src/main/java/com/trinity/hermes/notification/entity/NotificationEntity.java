package com.trinity.hermes.notification.entity;

import com.trinity.hermes.notification.model.enums.Channel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications") // Hibernate applies default_schema=backend automatically
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  private String recipient;

  @Column(nullable = false)
  private String subject;

  @Column(columnDefinition = "TEXT")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Channel channel;

  // Primitive boolean: Lombok generates isRead() getter, Jackson serializes as "read"
  @Column(name = "is_read", nullable = false)
  private boolean isRead;

  @Column(name = "qr_code_id")
  private String qrCodeId;

  /** Optional deep-link shown as a CTA in the notification detail view. */
  @Column(name = "action_url")
  private String actionUrl;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** Soft-delete timestamp. Non-null = in bin. Hard-deleted after 30 days. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  protected void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now(ZoneId.of("Europe/Dublin"));
    }
  }
}
