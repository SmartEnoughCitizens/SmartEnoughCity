package com.trinity.hermes.indicators.cycle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "dublin_bikes_stations", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DublinBikesStation {

  @Id
  @Column(name = "station_id")
  private Integer stationId;

  @Column(name = "system_id")
  private String systemId;

  private String name;

  @Column(name = "short_name")
  private String shortName;

  private String address;

  @Column(precision = 10, scale = 6)
  private BigDecimal latitude;

  @Column(precision = 10, scale = 6)
  private BigDecimal longitude;

  private Integer capacity;

  @Column(name = "region_id")
  private String regionId;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;
}
