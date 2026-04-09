package com.trinity.hermes.indicators.cycle.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationRiskScoreDTO {

  private int stationId;
  private String name;
  private BigDecimal latitude;
  private BigDecimal longitude;

  /** Probability (0.0–1.0) that this station will be empty within 2 hours. */
  private double emptyRisk2h;

  /** Probability (0.0–1.0) that this station will be full within 2 hours. */
  private double fullRisk2h;

  /** When this score was last computed by the inference engine. */
  private Instant scoredAt;

  /** When the underlying model was last trained. */
  private Instant modelTrainedAt;
}
