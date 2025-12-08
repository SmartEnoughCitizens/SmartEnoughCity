package com.trinity.hermes.disruptionmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Represents an alternative route option for a disruption
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlternativeRoute {

    private String routeId;
    private String routeName;
    private List<String> transportModes; // Combined modes like [BUS, METRO]
    private List<String> stops; // Sequence of stops

    // Route Metrics
    private Integer estimatedDurationMinutes;
    private Integer additionalTimeMinutes; // Compared to original route
    private Double distanceKm;
    private Double estimatedCost;

    // Route Quality
    private Integer comfortScore; // 1-10
    private Integer reliabilityScore; // 1-10
    private Integer crowdingLevel; // 1-10

    // Recommendations
    private Boolean recommended; // Is this a recommended option?
    private Integer priority; // Lower number = higher priority
    private String notes; // Additional information
}
