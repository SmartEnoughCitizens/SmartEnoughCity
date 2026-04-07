package com.trinity.hermes.approval.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Generic approval request that any indicator (train, bus, cycle…) can raise. Callers pass their
 * simulation/recommendation payload as JSON; the approval workflow (notify City_Manager → review →
 * notify requester) is handled centrally.
 */
@Entity
@Table(name = "approval_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** E.g. "train", "bus", "cycle" — used for role resolution and display. */
  @Column(nullable = false)
  private String indicator;

  /** Keycloak username of the person who raised the request. */
  @Column(nullable = false)
  private String requestedBy;

  /** PENDING | APPROVED | DENIED */
  @Column(nullable = false)
  @Builder.Default
  private String status = "PENDING";

  /**
   * Arbitrary JSON payload provided by the caller — simulation corridors, recommended changes,
   * whatever the indicator needs to describe the request.
   */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String payloadJson;

  /**
   * Optional human-readable summary (shown in notification and table) so reviewers understand the
   * request without parsing payloadJson.
   */
  @Column(columnDefinition = "TEXT")
  private String summary;

  /** Keycloak username of the reviewer (City_Manager). */
  private String reviewedBy;

  @Column(columnDefinition = "TEXT")
  private String reviewNote;

  /**
   * Optional deep-link back to the dashboard view for this indicator. Provided by the caller at
   * creation time; included in all approval notifications.
   */
  @Column private String actionUrl;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp private ZonedDateTime updatedAt;
}
