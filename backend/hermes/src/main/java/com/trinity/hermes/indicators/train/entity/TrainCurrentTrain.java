package com.trinity.hermes.indicators.train.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "irish_rail_current_trains", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainCurrentTrain {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "train_code")
    private String trainCode;

    @Column(name = "train_date")
    private LocalDate trainDate;

    @Column(name = "train_status")
    private String trainStatus;

    @Column(name = "train_type")
    private String trainType;

    @Column(name = "direction")
    private String direction;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "public_message")
    private String publicMessage;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
