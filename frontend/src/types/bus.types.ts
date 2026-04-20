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

export interface BusCommonDelay {
  routeId: string;
  routeShortName: string;
  routeLongName: string;
  avgDelayMinutes: number;
}

export interface BusRouteBreakdown {
  stopId: string;
  avgDelayMinutes: number;
  maxDelayMinutes: number;
  tripCount: number;
}

export interface BusStopSummary {
  id: string;
  code: number;
  name: string;
  lat: number;
  lon: number;
}

export interface BusNewStopRecommendation {
  routeId: string;
  routeShortName: string;
  routeLongName: string;
  stopA: BusStopSummary;
  stopB: BusStopSummary;
  candidateLat: number;
  candidateLon: number;
  populationScore: number | null;
  publicSpaceScore: number | null;
  combinedScore: number;
}

export interface BusRouteShapePoint {
  sequence: number;
  lat: number;
  lon: number;
  distTraveled: number;
}

export interface BusRouteStop {
  sequence: number;
  stopId: string;
  code: number | null;
  name: string | null;
  lat: number | null;
  lon: number | null;
  headsign: string | null;
}

export interface BusRouteDetail {
  routeId: string;
  agencyId: number;
  shortName: string;
  longName: string;
  representativeTripId: string;
  shapeId: string;
  shape: BusRouteShapePoint[];
  stops: BusRouteStop[];
}
