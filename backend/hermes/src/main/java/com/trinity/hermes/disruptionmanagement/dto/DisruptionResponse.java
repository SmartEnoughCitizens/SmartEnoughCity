package com.trinity.hermes.disruptionmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisruptionResponse {
  private Long id;
  private String name;
  private String description;
  private String status;
  private String severity;
  private String disruptionType;
  private List<String> affectedTransportModes;
  private List<String> affectedRoutes;
  private String affectedArea;
  private Double latitude;
  private Double longitude;
  private LocalDateTime detectedAt;
  private LocalDateTime estimatedEndTime;
  private Integer delayMinutes;
  private Boolean notificationSent;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<CauseDTO> causes;
  private List<AlternativeDTO> alternatives;
}
