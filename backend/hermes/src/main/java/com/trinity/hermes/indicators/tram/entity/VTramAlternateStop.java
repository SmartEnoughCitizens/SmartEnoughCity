package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Getter
@NoArgsConstructor
@Table(name = "v_tram_alternate_stops", schema = "external_data")
public class VTramAlternateStop {

    @Id
    @Column(name = "stop_id")
    private String stopId;

    @Column(name = "tram_stop_id")
    private String tramStopId;

    @Column(name = "transport_type")
    private String transportType;

    @Column(name = "stop_name")
    private String stopName;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "available_bikes")
    private Integer availableBikes;

    @Column(name = "capacity")
    private Integer capacity;
}