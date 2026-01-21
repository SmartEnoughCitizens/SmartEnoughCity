package com.trinity.hermes.disruptionmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisruptionDetectionResponse {
  private boolean success;
  private Long disruptionId;
  private String message;
}
