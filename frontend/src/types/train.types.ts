/**
 * Train station types matching backend DTOs
 */

export interface TrainStation {
  id: number;
  stationCode: string;
  stationDesc: string;
  stationAlias?: string;
  lat: number;
  lon: number;
  stationType?: string;
}

export interface TrainDashboardResponse {
  indicatorType: string;
  totalRecords: number;
  data: TrainStation[];
}

export interface TrainKpis {
  totalStations: number;
  liveTrainsRunning: number;
  onTimePct: number;
  avgDelayMinutes: number;
}

export interface TrainLiveTrain {
  trainCode: string;
  direction?: string;
  trainType?: string;
  status?: string;
  lat: number;
  lon: number;
  publicMessage?: string;
}

export interface TrainServiceStats {
  reliabilityPct: number;
  lateArrivalPct: number;
  avgDueMinutes: number;
}

export type TimeOfDay =
  | "MORNING_PEAK"
  | "MIDDAY"
  | "AFTERNOON"
  | "EVENING_PEAK"
  | "NIGHT";

export type SeverityLevel = "SEVERE" | "MODERATE" | "MINOR";

export interface TrainDelayPattern {
  stationCode: string;
  stationDesc: string;
  lat: number;
  lon: number;
  /** Departure terminal — Irish Rail "origin" field. */
  origin: string;
  /** Arrival terminal — Irish Rail "destination" field. */
  destination: string;
  trainType?: string;
  timeOfDay: TimeOfDay;
  avgDelayMinutes: number;
  maxDelayMinutes: number;
  occurrenceCount: number;
  latePercent: number;
  /** SEVERE ≥ 10 min · MODERATE 5–10 min · MINOR 1–5 min */
  severityLevel: SeverityLevel;
}

export interface TrainStationUtilization {
  stationCode: string;
  stationDesc: string;
  lat: number;
  lon: number;
  /** Number of distinct active train services at this station in the latest snapshot. */
  trainServiceCount: number;
  /** Average delay in minutes across those services. */
  avgDelayMinutes: number;
  /** "HIGH" | "MEDIUM" | "LOW" relative to the Dublin-wide mean. */
  utilizationLevel: "HIGH" | "MEDIUM" | "LOW";
}
