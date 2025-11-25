package com.trinity.hermes.dataanalyzer.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Indicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String indicatorType; // e.g., "bus", "car", "energy", "water"

    @Column(nullable = false)
    private String metricName;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private String unit;

    private String location;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}