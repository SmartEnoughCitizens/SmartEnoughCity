package com.trinity.hermes.indicators.train.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "train_stops", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsStop {

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "code")
  private String code;

  @Column(name = "name")
  private String name;

  @Column(name = "lat")
  private Double lat;

  @Column(name = "lon")
  private Double lon;
}
