package com.trinity.hermes.indicators.train.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "train_approval_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainApprovalRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String requestedBy;

  /** PENDING | APPROVED | DENIED */
  @Column(nullable = false)
  @Builder.Default
  private String status = "PENDING";

  /** JSON array of corridor objects: [{originStopId, destinationStopId, trainCount}] */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String corridorsJson;

  /** JSON object with simulation summary metrics */
  @Column(columnDefinition = "TEXT")
  private String metricsJson;

  private String reviewedBy;

  @Column(columnDefinition = "TEXT")
  private String reviewNote;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  private ZonedDateTime updatedAt;
}
