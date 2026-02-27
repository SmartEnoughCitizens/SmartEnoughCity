/**
 * Bus trip update types matching backend DTOs
 */

export interface BusTripUpdate {
  id: number;
  entityId: string;
  tripId: string;
  routeId: string;
  startTime: string;
  startDate: string;
  stopSequence: string;
  stopId: string;
  arrivalDelay: number | null;
  departureDelay: number | null;
  scheduleRelationship: string;
}

export interface DelayStatistics {
  routeId: string;
  totalTrips: number;
  averageArrivalDelay?: number;
  maxArrivalDelay?: number;
  minArrivalDelay?: number;
  averageDepartureDelay?: number;
  maxDepartureDelay?: number;
  minDepartureDelay?: number;
}

export interface BusDashboardResponse {
  indicatorType: string;
  routeId?: string;
  totalRecords: number;
  totalRoutes?: number;
  routes?: string[];
  data: BusTripUpdate[];
  statistics?: DelayStatistics;
}

export interface BusKpis {
  totalBusesRunning: number;
  activeDelays: number;
  fleetUtilizationPct: number;
  sustainabilityScore: number;
}

export interface BusLiveVehicle {
  vehicleId: number;
  routeShortName: string;
  latitude: number;
  longitude: number;
  status: string;
  occupancyPct: number;
  delaySeconds: number;
}

export interface BusRouteUtilization {
  routeId: string;
  routeShortName: string;
  routeLongName: string;
  utilizationPct: number;
  activeVehicles: number;
  status: string;
}

export interface BusSystemPerformance {
  reliabilityPct: number;
  lateArrivalPct: number;
}
