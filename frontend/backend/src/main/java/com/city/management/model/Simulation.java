package com.city.management.model;

// import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
// @AllArgsConstructor
public class Simulation {
    private Long id;
    private String name;
    private String type;
    private String status;
    private Map<String, Object> parameters;
    private Map<String, Object> results;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String createdBy;
}
