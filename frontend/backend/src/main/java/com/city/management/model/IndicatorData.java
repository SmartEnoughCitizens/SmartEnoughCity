package com.city.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorData {
    private String mode;
    private Map<String, Object> summary;
    private List<TimeSeriesData> timeSeries;
    private Map<String, Object> metrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesData {
        private String timestamp;
        private double value;
        private Map<String, Object> metadata;
    }
}
