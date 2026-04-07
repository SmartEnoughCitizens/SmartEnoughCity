package com.trinity.hermes.disruptionmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nearby alternative transport option — populated by AlternativeTransportService. */
@Entity
@Table(name = "disruption_alternatives")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisruptionAlternative {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disruption_id", nullable = false)
  private Disruption disruption;

  /** bus, rail, bike */
  @Column(nullable = false)
  private String mode;

  @Column(length = 500)
  private String description;

  private Integer etaMinutes;

  private String stopName;

  private Integer availabilityCount;

  private Double lat;

  private Double lon;
}
