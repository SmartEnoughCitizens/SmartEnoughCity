package com.trinity.hermes.indicators.cycle.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NetworkKpiDTO {

  private Integer rebalancingNeedCount;
  private Double networkImbalanceScore;
  private Double avgHourlyTurnoverRate;
  private Long dailyTripsEstimate;
  private Double weekdayAvgUsageRate;
  private Double weekendAvgUsageRate;
  private Map<Integer, Double> hourlyUsageProfile;
  private List<StationTimeSeriesDTO> dailyTrend;

  public NetworkKpiDTO(
      Integer rebalancingNeedCount,
      Double networkImbalanceScore,
      Double avgHourlyTurnoverRate,
      Long dailyTripsEstimate,
      Double weekdayAvgUsageRate,
      Double weekendAvgUsageRate,
      Map<Integer, Double> hourlyUsageProfile,
      List<StationTimeSeriesDTO> dailyTrend) {
    this.rebalancingNeedCount = rebalancingNeedCount;
    this.networkImbalanceScore = networkImbalanceScore;
    this.avgHourlyTurnoverRate = avgHourlyTurnoverRate;
    this.dailyTripsEstimate = dailyTripsEstimate;
    this.weekdayAvgUsageRate = weekdayAvgUsageRate;
    this.weekendAvgUsageRate = weekendAvgUsageRate;
    this.hourlyUsageProfile = hourlyUsageProfile != null ? new HashMap<>(hourlyUsageProfile) : null;
    this.dailyTrend = dailyTrend != null ? new ArrayList<>(dailyTrend) : null;
  }

  public Map<Integer, Double> getHourlyUsageProfile() {
    return hourlyUsageProfile != null ? Collections.unmodifiableMap(hourlyUsageProfile) : null;
  }

  public void setHourlyUsageProfile(Map<Integer, Double> hourlyUsageProfile) {
    this.hourlyUsageProfile = hourlyUsageProfile != null ? new HashMap<>(hourlyUsageProfile) : null;
  }

  public List<StationTimeSeriesDTO> getDailyTrend() {
    return dailyTrend != null ? Collections.unmodifiableList(dailyTrend) : null;
  }

  public void setDailyTrend(List<StationTimeSeriesDTO> dailyTrend) {
    this.dailyTrend = dailyTrend != null ? new ArrayList<>(dailyTrend) : null;
  }
}
