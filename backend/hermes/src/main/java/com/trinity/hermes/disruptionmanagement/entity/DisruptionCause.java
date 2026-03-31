package com.trinity.hermes.disruptionmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cause correlated with a disruption — populated by CauseCorrelationService. */
@Entity
@Table(name = "disruption_causes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisruptionCause {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disruption_id", nullable = false)
  private Disruption disruption;

  /** EVENT, CONGESTION, CROSS_MODE */
  @Column(nullable = false)
  private String causeType;

  @Column(length = 500)
  private String causeDescription;

  /** HIGH, MEDIUM, LOW */
  @Column(nullable = false)
  private String confidence;
}
