package com.trinity.hermes.indicators.train.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "irish_rail_train_movements", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TrainMovement.TrainMovementId.class)
public class TrainMovement {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TrainMovementId implements Serializable {
    private String trainCode;
    private LocalDate trainDate;
    private String locationCode;
  }

  @Id
  @Column(name = "train_code")
  private String trainCode;

  @Id
  @Column(name = "train_date")
  private LocalDate trainDate;

  @Id
  @Column(name = "location_code")
  private String locationCode;

  @Column(name = "location_full_name")
  private String locationFullName;

  @Column(name = "location_order")
  private Integer locationOrder;

  @Column(name = "train_origin")
  private String trainOrigin;

  @Column(name = "train_destination")
  private String trainDestination;

  @Column(name = "scheduled_arrival")
  private LocalTime scheduledArrival;

  @Column(name = "scheduled_departure")
  private LocalTime scheduledDeparture;
}
