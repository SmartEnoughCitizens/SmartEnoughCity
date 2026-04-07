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

export interface TrainDelay {
  trainCode: string;
  origin: string;
  destination: string;
  direction: string;
  totalAvgDelayMinutes: number;
}

export interface TrainRoute {
  routeName: string;
  shortName: string;
  /** Ordered [[lat, lon], ...] waypoints */
  stops: [number, number][];
  /** GTFS stop IDs parallel to stops — used for demand colouring */
  stopIds: string[];
}

export interface StationDemand {
  stopId: string;
  name: string;
  lat: number;
  lon: number;
  tripCount: number;
  ridershipCount: number;
  catchmentPopulation: number;
  stationType: string | null;
  footfallCount: number;
  /** Normalised sub-scores (0-1) for popup breakdown */
  normRidership: number;
  normUptake: number;
  normPressure: number;
  normFootfall: number;
  rawPressure: number;
  maxPressure: number;
  /** Composite 0.0 (low) → 1.0 (high) */
  demandScore: number;
}

export interface TrainDemandCorridor {
  originStopId: string;
  destinationStopId: string;
  trainCount: number;
}

export interface TrainDemandSimulateRequest {
  corridors: TrainDemandCorridor[];
}

export interface TrainDemandSimulateResponse {
  baseDemand: StationDemand[];
  simulatedDemand: StationDemand[];
  affectedStopIds: string[];
}

export interface TrainRecommendation {
  id: number;
  indicator: string;
  recommendation: string;
  simulation: string;
  usecase: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}
