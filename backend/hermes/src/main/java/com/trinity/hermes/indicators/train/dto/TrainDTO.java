package com.trinity.hermes.indicators.train.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainDTO {
  private Integer id;
  private String stationCode;
  private String stationDesc;
  private String stationAlias;
  private Double lat;
  private Double lon;
  private String stationType;
}
