package com.trinity.hermes.disruptionmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlternativeDTO {
  private Long id;
  private String mode;
  private String description;
  private Integer etaMinutes;
  private String stopName;
  private Integer availabilityCount;
  private Double lat;
  private Double lon;

  /** Google Maps walking directions URL from the disruption point to this alternative. */
  private String googleMapsWalkingUrl;
}
