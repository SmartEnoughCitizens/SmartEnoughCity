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
