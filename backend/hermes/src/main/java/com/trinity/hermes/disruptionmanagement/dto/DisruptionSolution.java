package com.trinity.hermes.disruptionmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a compiled solution for a disruption, including all alternative routes and recommended
 * actions
 */
@Data
@NoArgsConstructor
public class DisruptionSolution {

  private Long disruptionId;
  private String disruptionType;
  private String severity;
  private String description;
  private String affectedArea;

  // Alternative Routes (Simplified for notification)
  private List<String> alternativeRoutes;

  // Recommendations (Simplified for notification)
  private String primaryRecommendation;
  private List<String> secondaryRecommendations;

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

    public DisruptionSolution(
            Long disruptionId,
            String disruptionType,
            String severity,
            String description,
            String affectedArea,
            List<String> alternativeRoutes,
            String primaryRecommendation,
            List<String> secondaryRecommendations,
            String actionSummary,
            List<String> stepByStepInstructions,
            String estimatedImpact,
            LocalDateTime calculatedAt,
            String calculationMethod,
            Integer numberOfOptionsEvaluated,
            Boolean readyForNotification,
            List<String> affectedUserGroups) {

        this.disruptionId = disruptionId;
        this.disruptionType = disruptionType;
        this.severity = severity;
        this.description = description;
        this.affectedArea = affectedArea;

        this.alternativeRoutes = copyList(alternativeRoutes);

        this.primaryRecommendation = primaryRecommendation;
        this.secondaryRecommendations = copyList(secondaryRecommendations);

        this.actionSummary = actionSummary;
        this.stepByStepInstructions = copyList(stepByStepInstructions);
        this.estimatedImpact = estimatedImpact;

        this.calculatedAt = calculatedAt;
        this.calculationMethod = calculationMethod;
        this.numberOfOptionsEvaluated = numberOfOptionsEvaluated;

        this.readyForNotification = readyForNotification;
        this.affectedUserGroups = copyList(affectedUserGroups);
    }

    public void setAlternativeRoutes(List<String> alternativeRoutes) {
        this.alternativeRoutes = copyList(alternativeRoutes);
    }

    public void setSecondaryRecommendations(List<String> secondaryRecommendations) {
        this.secondaryRecommendations = copyList(secondaryRecommendations);
    }

    public void setStepByStepInstructions(List<String> stepByStepInstructions) {
        this.stepByStepInstructions = copyList(stepByStepInstructions);
    }

    public void setAffectedUserGroups(List<String> affectedUserGroups) {
        this.affectedUserGroups = copyList(affectedUserGroups);
    }

    // ✅ Defensive getters (copy OUT)
    public List<String> getAlternativeRoutes() {
        return alternativeRoutes == null ? null : List.copyOf(alternativeRoutes);
    }

    public List<String> getSecondaryRecommendations() {
        return secondaryRecommendations == null ? null : List.copyOf(secondaryRecommendations);
    }

    public List<String> getStepByStepInstructions() {
        return stepByStepInstructions == null ? null : List.copyOf(stepByStepInstructions);
    }

    public List<String> getAffectedUserGroups() {
        return affectedUserGroups == null ? null : List.copyOf(affectedUserGroups);
    }

    private static <T> List<T> copyList(List<T> in) {
        return in == null ? null : List.copyOf(in); // Java 10+
    }
}
