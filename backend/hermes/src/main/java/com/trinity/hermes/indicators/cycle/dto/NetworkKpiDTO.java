package com.trinity.hermes.indicators.cycle.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkKpiDTO {

  private Integer rebalancingNeedCount;
  private Double networkImbalanceScore;
  private Double avgHourlyTurnoverRate;
  private Long dailyTripsEstimate;
  private Double weekdayAvgUsageRate;
  private Double weekendAvgUsageRate;
  private Map<Integer, Double> hourlyUsageProfile;
  private List<StationTimeSeriesDTO> dailyTrend;
}
