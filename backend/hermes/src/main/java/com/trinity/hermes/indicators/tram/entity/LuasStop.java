package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "luas_stops", schema = "external_data") // Use schema to match production
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LuasStop {

    @Id
    private String stopId;

    private String line;
    private String name;
    private String pronunciation;
    private Boolean parkRide;
    private Boolean cycleRide;
    private Double lat;
    private Double lon;

    // updated_at omitted for brevity or can be added
}
