package com.trinity.hermes.indicators.car.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighTrafficPointsId implements Serializable {

  private Integer siteId;
  private Timestamp endTime;
}
