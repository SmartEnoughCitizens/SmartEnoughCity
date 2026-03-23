package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramHourlyDistributionDTO {

  private String timeLabel;
  private String line;
  private Double percentage;
}
