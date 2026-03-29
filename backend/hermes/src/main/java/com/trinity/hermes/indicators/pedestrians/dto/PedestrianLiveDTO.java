package com.trinity.hermes.indicators.pedestrians.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedestrianLiveDTO {

  private Integer siteId;
  private String siteName;
  private Double lat;
  private Double lon;
  private Long totalCount;
  private OffsetDateTime lastUpdated;
}
