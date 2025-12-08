package com.trinity.hermes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private NotificationType type;
    private String message;
    private Priority priority;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private boolean read;
}
