package com.trinity.hermes.disruptionmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CauseDTO {
  private Long id;
  private String causeType;
  private String causeDescription;
  private String confidence;
}
