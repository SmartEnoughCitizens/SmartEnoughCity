package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_stops", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusStop {

  @Id private String id;

  @Column(nullable = false)
  private Integer code;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(nullable = false)
  private Double lat;

  @Column(nullable = false)
  private Double lon;
}
