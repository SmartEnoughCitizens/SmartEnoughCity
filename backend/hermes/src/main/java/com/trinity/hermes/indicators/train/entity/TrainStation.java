package com.trinity.hermes.indicators.train.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "irish_rail_stations", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainStation {

  @Id
  @Column(name = "station_id")
  private Integer id;

  @Column(name = "station_code")
  private String stationCode;

  @Column(name = "station_desc")
  private String stationDesc;

  @Column(name = "station_alias")
  private String stationAlias;

  @Column(name = "lat")
  private Double lat;

  @Column(name = "lon")
  private Double lon;

  @Column(name = "station_type")
  private String stationType;
}
