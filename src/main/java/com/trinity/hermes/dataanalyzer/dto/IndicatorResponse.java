package com.trinity.hermes.dataanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorResponse {

    private String indicatorType;
    private int totalRecords;
    private List<IndicatorDTO> data;
    private StatisticsSummary statistics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsSummary {
        private Double average;
        private Double min;
        private Double max;
        private Double latest;
    }
}