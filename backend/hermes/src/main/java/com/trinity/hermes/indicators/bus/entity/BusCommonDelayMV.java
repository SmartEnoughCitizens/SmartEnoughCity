package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "bus_common_delays", schema = "backend")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusCommonDelayMV {

  @Id private Long id;

  @Column(name = "period")
  private String period;

  @Column(name = "route_id")
  private String routeId;

  @Column(name = "route_short_name")
  private String routeShortName;

  @Column(name = "route_long_name")
  private String routeLongName;

  @Column(name = "avg_delay_minutes")
  private Double avgDelayMinutes;
}
