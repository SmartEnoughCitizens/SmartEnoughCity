package com.trinity.hermes.disruptionmanagement.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisruptionResponse {
  private Long id;
  private String name;
  private String description;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
