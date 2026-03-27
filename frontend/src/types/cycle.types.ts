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

// ── CycleMetricsController DTOs ──────────────────────────────────────────────

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
  bikeAvailabilityPct: number;
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
  avgUsageRate: number;
}

export interface StationEventDTO {
  stationId: number;
  stationName: string;
  eventTime: string;
  availableBikes: number;
  prevAvailableBikes: number;
  eventType: "EMPTY" | "FULL";
}

// ── Demand Analysis DTOs ──────────────────────────────────────────────────────

export interface HourlyNetworkProfileDTO {
  hourOfDay: number; // 0–23 in Europe/Dublin local time
  avgTurnover: number; // total natural bike movements (ABS delta 1–5) across all stations
  stationCount: number;
}

export type StationClassification =
  | "MORNING_PEAK"
  | "AFTERNOON_PEAK"
  | "EVENING_PEAK"
  | "OFF_PEAK";

export interface StationClassificationDTO {
  stationId: number;
  name: string;
  peakHour: number; // 0–23
  peakUsage: number; // total bike movements at peak hour
  classification: StationClassification;
}

/** Estimated origin → destination trip pair derived from snapshot availability changes. */
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
  distanceKm: number;
}

/** Per-station, per-hour natural bike turnover — one row per (station, hour) pair. */
export interface StationHourlyUsageDTO {
  stationId: number;
  name: string;
  hourOfDay: number; // 0–23 Europe/Dublin local time
  avgTurnover: number; // total natural bike movements (ABS delta 1–5) for that hour
}

/** Rebalancing suggestion: move bikes FROM full source station TO empty target station */
export interface RebalanceSuggestionDTO {
  sourceStationId: number;
  sourceName: string;
  sourceLat: number;
  sourceLon: number;
  sourceBikes: number;
  targetStationId: number;
  targetName: string;
  targetLat: number;
  targetLon: number;
  targetCapacity: number;
  distanceKm: number;
}
