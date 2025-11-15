package com.city.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private int totalUsers;
    private int activeSimulations;
    private int totalIncidents;
    private double systemHealth;
    private List<MetricData> recentMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricData {
        private String timestamp;
        private String metric;
        private double value;
    }
}
