package com.trinity.hermes.disruptionmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a compiled solution for a disruption, including all alternative
 * routes
 * and recommended actions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisruptionSolution {

    private Long disruptionId;
    private String disruptionType;
    private String severity;
    private String description;
    private String affectedArea;

    // Alternative Routes
    private List<AlternativeRoute> alternativeRoutes;

    // Recommendations
    private AlternativeRoute primaryRecommendation;
    private List<AlternativeRoute> secondaryRecommendations;

    // User Guidance
    private String actionSummary; // "Take Metro Line 2 instead of Bus 15"
    private List<String> stepByStepInstructions;
    private String estimatedImpact; // "15 minutes additional travel time"

    // Solution Metadata
    private LocalDateTime calculatedAt;
    private String calculationMethod; // Algorithm used
    private Integer numberOfOptionsEvaluated;

    // Notification Information
    private Boolean readyForNotification;
    private List<String> affectedUserGroups; // Which users should be notified
}
