package com.trinity.hermes.dataanalyzer.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorDTO {

    private Long id;
    private String indicatorType;
    private String metricName;
    private Double value;
    private String unit;
    private String location;
    private LocalDateTime timestamp;
    private String metadata;
}