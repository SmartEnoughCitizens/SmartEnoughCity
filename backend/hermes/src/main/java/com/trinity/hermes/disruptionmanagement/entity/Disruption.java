package com.trinity.hermes.disruptionmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Disruption entity representing a transport disruption incident
 */
@Entity
@Table(name = "disruptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Disruption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String status; // DETECTED, ANALYZING, RESOLVED, CANCELLED

    // Disruption Classification
    @Column(nullable = false)
    private String disruptionType; // DELAY, CANCELLATION, CONGESTION, CONSTRUCTION, EVENT, ACCIDENT

    @Column(nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    // Location Information
    private Double latitude;
    private Double longitude;
    private String affectedArea;

    // Transport Information (stored as JSON)
    @Convert(converter = ListToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> affectedTransportModes; // BUS, TRAM, TRAIN, METRO

    @Convert(converter = ListToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> affectedRoutes;

    @Convert(converter = ListToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> affectedStops;

    // Time Information
    @Column(nullable = false)
    private LocalDateTime detectedAt;

    private LocalDateTime startTime;
    private LocalDateTime estimatedEndTime;
    private LocalDateTime resolvedAt;

    // Solution Information
    @Column(columnDefinition = "TEXT")
    private String alternativeRoutesCalculated; // JSON string or comma-separated

    @Column(columnDefinition = "TEXT")
    private String recommendedSolutions; // JSON string

    @Column(nullable = false)
    private Boolean notificationSent = false;

    // Source Information
    private String dataSource; // PYTHON_SERVICE, MANUAL, API
    private String sourceReferenceId;

    // Additional Context
    @Column(columnDefinition = "TEXT")
    private String eventDetails; // For events

    @Column(columnDefinition = "TEXT")
    private String constructionDetails; // For construction work

    private Integer delayMinutes; // For delays
}
