package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "tram_hourly_distribution", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramHourlyDistribution {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "statistic", nullable = false)
  private String statistic;

  @Column(name = "statistic_label", nullable = false)
  private String statisticLabel;

  @Column(name = "year", nullable = false)
  private String year;

  @Column(name = "line_code", nullable = false)
  private String lineCode;

  @Column(name = "line_label", nullable = false)
  private String lineLabel;

  @Column(name = "time_code", nullable = false)
  private String timeCode;

  @Column(name = "time_label", nullable = false)
  private String timeLabel;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "value")
  private Double value;
}
