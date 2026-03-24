package com.trinity.hermes.indicators.car.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JunctionEmissionDTO {

  private Integer siteId;
  private Double lat;
  private Double lon;
  private String dayType;
  private String timeSlot;

  // Vehicle type volumes derived from avg_volume
  private double carVolume;
  private double lcvVolume;
  private double busVolume;
  private double hgvVolume;
  private double motorcycleVolume;

  // Total CO2 emission in grams (moving + idle) across all vehicle types and bands
  private double totalEmissionG;
}
