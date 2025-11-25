package com.trinity.hermes.dataanalyzer.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationEngineRequest {

    private String indicatorType; // bus, car, energy, water, etc.
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer limit; // Number of records to return
    private String aggregationType; // raw, hourly, daily, weekly
}