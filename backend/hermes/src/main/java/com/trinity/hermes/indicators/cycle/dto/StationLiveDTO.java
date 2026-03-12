package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationLiveDTO {

  private Integer stationId;
  private String name;
  private String shortName;
  private String address;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private Integer capacity;
  private String regionId;
  private Integer availableBikes;
  private Integer availableDocks;
  private Integer disabledBikes;
  private Integer disabledDocks;
  private Boolean isInstalled;
  private Boolean isRenting;
  private Boolean isReturning;
  private Instant lastReported;
  private Instant snapshotTimestamp;
  private Double bikeAvailabilityPct;
  private Double dockAvailabilityPct;
  private String statusColor;
  private Boolean isEmpty;
  private Boolean isFull;
}
