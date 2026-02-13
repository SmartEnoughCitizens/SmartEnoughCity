package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_calendar_schedule", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusCalendarSchedule {

  @Id
  @Column(name = "entry_id")
  private Integer entryId;

  @Column(name = "service_id", nullable = false)
  private Integer serviceId;

  @Column(nullable = false)
  private Boolean monday;

  @Column(nullable = false)
  private Boolean tuesday;

  @Column(nullable = false)
  private Boolean wednesday;

  @Column(nullable = false)
  private Boolean thursday;

  @Column(nullable = false)
  private Boolean friday;

  @Column(nullable = false)
  private Boolean saturday;

  @Column(nullable = false)
  private Boolean sunday;

  @Column(name = "start_date", nullable = false)
  private Date startDate;

  @Column(name = "end_date", nullable = false)
  private Date endDate;
}
