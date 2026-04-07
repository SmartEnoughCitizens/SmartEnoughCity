package com.trinity.hermes.indicators.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "venues", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venue {

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "ticketmaster_id")
  private String ticketmasterId;

  @Column(name = "name")
  private String name;

  @Column(name = "address")
  private String address;

  @Column(name = "city")
  private String city;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "capacity")
  private Integer capacity;
}
