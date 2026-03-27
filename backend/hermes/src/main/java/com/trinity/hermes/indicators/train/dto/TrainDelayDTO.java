package com.trinity.hermes.indicators.train.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainDelayDTO {
  private String trainCode;
  private String origin;
  private String destination;
  private String direction;
  private Double totalAvgDelayMinutes;
}
