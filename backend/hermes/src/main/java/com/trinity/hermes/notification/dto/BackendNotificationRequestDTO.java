package com.trinity.hermes.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class BackendNotificationRequestDTO {
    private String userId;
    private String qrid;
    private String userName;
    @JsonProperty("data_indicator")
    private String dataIndicator;
    private Map<String, Object> recommendation;
    private String subject;
    private String body;
    private Map<String, Object> metadata;
    private String priority;

}
