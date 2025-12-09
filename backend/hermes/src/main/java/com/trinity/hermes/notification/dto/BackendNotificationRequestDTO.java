package com.trinity.hermes.notification.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BackendNotificationRequestDTO {
    private String userId;
    private String userName;
    private String serviceType;
    private String message;
    private Map<String, Object> metadata;
    private String priority;

}
