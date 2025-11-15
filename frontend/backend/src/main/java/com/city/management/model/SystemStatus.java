package com.city.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatus {
    private String status;
    private String version;
    private long uptime;
    private Map<String, ServiceStatus> services;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatus {
        private String status;
        private String message;
        private long responseTime;
    }
}
