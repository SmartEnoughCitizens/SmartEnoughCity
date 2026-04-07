package com.trinity.hermes.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trinity.hermes.notification.model.enums.Channel;
import java.util.Map;
import lombok.Data;

@Data
public class BackendNotificationRequestDTO {
  private String userId;
  private String qrid;
  private String userName;

  @JsonProperty("data_indicator")
  private String dataIndicator;

  private Map<String, Object> recommendation;
  private Map<String, Object> simulation;
  private String subject;
  private String body;
  private Map<String, Object> metadata;
  private String priority;
  private Channel channel;
  private String actionUrl;

  public Map<String, Object> getMetadata() {
    return metadata == null ? null : Map.copyOf(metadata);
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata == null ? null : Map.copyOf(metadata);
  }

  public Map<String, Object> getRecommendation() {
    return recommendation == null ? null : Map.copyOf(recommendation);
  }

  public void setRecommendation(Map<String, Object> recommendation) {
    this.recommendation = recommendation == null ? null : Map.copyOf(recommendation);
  }

  public Map<String, Object> getSimulation() {
    return simulation == null ? null : Map.copyOf(simulation);
  }

  public void setSimulation(Map<String, Object> simulation) {
    this.simulation = simulation == null ? null : Map.copyOf(simulation);
  }
}
