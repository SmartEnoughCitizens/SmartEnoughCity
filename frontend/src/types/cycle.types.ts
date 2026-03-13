/**
 * Cycle station types matching backend DTOs
 */

export interface CycleStation {
  id: number;
  stationId: string;
  name: string;
  address: string;
  capacity: number;
  numBikesAvailable: number;
  numDocksAvailable: number;
  isInstalled: boolean;
  isRenting: boolean;
  isReturning: boolean;
  lastReported: number;
  lastReportedDt: string;
  lat: number;
  lon: number;
  occupancyRate: number;
}

export interface CycleStatistics {
  totalStations: number;
  averageBikesAvailable?: number;
  totalBikesAvailable?: number;
  maxBikesAtStation?: number;
  averageDocksAvailable?: number;
  totalDocksAvailable?: number;
  averageOccupancyRate?: number;
  stationsRenting?: number;
  stationsReturning?: number;
}

export interface CycleDashboardResponse {
  indicatorType: string;
  totalRecords: number;
  data: CycleStation[];
  statistics?: CycleStatistics;
}

// ── New types matching CycleMetricsController DTOs ──────────────────────────

export interface StationLiveDTO {
  stationId: number;
  name: string;
  shortName: string;
  address: string;
  latitude: number;
  longitude: number;
  capacity: number;
  regionId: string;
  availableBikes: number;
  availableDocks: number;
  disabledBikes: number;
  disabledDocks: number;
  isInstalled: boolean;
  isRenting: boolean;
  isReturning: boolean;
  lastReported: string;
  snapshotTimestamp: string;
  /** % of capacity that has bikes available (LOW = RED, station needs restocking) */
  bikeAvailabilityPct: number;
  /** % of capacity that has empty docks available (LOW = station is full, can't return) */
  dockAvailabilityPct: number;
  statusColor: "RED" | "YELLOW" | "GREEN";
  isEmpty: boolean;
  isFull: boolean;
}

export interface NetworkSummaryDTO {
  totalStations: number;
  activeStations: number;
  totalBikesAvailable: number;
  totalDocksAvailable: number;
  totalDisabledBikes: number;
  totalDisabledDocks: number;
  emptyStations: number;
  fullStations: number;
  avgNetworkFullnessPct: number;
  rebalancingNeedCount: number;
  dataAsOf: string;
}

export interface StationRankingDTO {
  stationId: number;
  name: string;
  regionId: string;
  capacity: number;
  avgUsageRate: number;
  avgAvailableBikes: number;
  avgAvailableDocks: number;
  emptyEventCount: number;
  fullEventCount: number;
}

export interface StationEventDTO {
  stationId: number;
  stationName: string;
  eventTime: string;
  availableBikes: number;
  prevAvailableBikes: number;
  eventType: "EMPTY" | "FULL";
}

export interface StationTimeSeriesDTO {
  period: string;
  avgAvailableBikes: number;
  avgAvailableDocks: number;
  usageRatePct: number;
}

export interface NetworkKpiDTO {
  rebalancingNeedCount: number;
  networkImbalanceScore: number;
  avgHourlyTurnoverRate: number;
  dailyTripsEstimate: number;
  weekdayAvgUsageRate: number;
  weekendAvgUsageRate: number;
  hourlyUsageProfile: Record<number, number>;
  dailyTrend: StationTimeSeriesDTO[];
}

/** Rebalancing suggestion: move bikes FROM full source station TO empty target station */
export interface RebalanceSuggestionDTO {
  sourceStationId: number;
  sourceName: string;
  sourceLat: number;
  sourceLon: number;
  /** Bikes currently at the full station */
  sourceBikes: number;
  targetStationId: number;
  targetName: string;
  targetLat: number;
  targetLon: number;
  targetCapacity: number;
  /** Straight-line distance in kilometres */
  distanceKm: number;
}

export interface StationODPairDTO {
  originStationId: number;
  originName: string;
  originLat: number;
  originLon: number;
  destStationId: number;
  destName: string;
  destLat: number;
  destLon: number;
  estimatedTrips: number;
}
