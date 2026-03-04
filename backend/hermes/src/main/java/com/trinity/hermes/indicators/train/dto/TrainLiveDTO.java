package com.trinity.hermes.indicators.train.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainLiveDTO {
    private String trainCode;
    private String direction;
    private String trainType;
    private String status;
    private Double lat;
    private Double lon;
    private String publicMessage;
}
