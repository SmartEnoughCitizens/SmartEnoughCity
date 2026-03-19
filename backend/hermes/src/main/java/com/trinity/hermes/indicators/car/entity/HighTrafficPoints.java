package com.trinity.hermes.indicators.car.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "traffic_volumes", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(HighTrafficPointsId.class)
public class HighTrafficPoints {

  @Id
  @Column(name = "site_id", nullable = false)
  private Integer siteId;

  @Id
  @Column(name = "end_time", nullable = false)
  private Timestamp endTime;

  @Column(name = "region", nullable = false)
  private String region;

  @Column(name = "sum_volume", nullable = false)
  private Double sumVolume;
}
