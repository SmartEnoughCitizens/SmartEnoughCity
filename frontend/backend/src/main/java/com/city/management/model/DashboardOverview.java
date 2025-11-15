package com.city.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverview {
    private Map<String, Object> summary;
    private List<ChartData> charts;
    private List<Alert> alerts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private String name;
        private String type;
        private List<DataPoint> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String label;
        // private double value;
        private int value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String severity;
        private String message;
        private String timestamp;
    }
}
