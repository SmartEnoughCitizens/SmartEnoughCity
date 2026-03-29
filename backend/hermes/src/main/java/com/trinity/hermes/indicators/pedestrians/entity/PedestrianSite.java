package com.trinity.hermes.indicators.pedestrians.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "pedestrian_counter_sites", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedestrianSite {

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "lat")
  private Double lat;

  @Column(name = "lon")
  private Double lon;

  @Column(name = "pedestrian_sensor")
  private Boolean pedestrianSensor;

  @Column(name = "bike_sensor")
  private Boolean bikeSensor;
}
