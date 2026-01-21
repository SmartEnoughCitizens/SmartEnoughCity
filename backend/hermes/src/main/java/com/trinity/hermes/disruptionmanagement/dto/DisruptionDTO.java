package com.trinity.hermes.disruptionmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisruptionDTO {
  private Long id;
  private String name;
  private String description;
  private String status;
}
