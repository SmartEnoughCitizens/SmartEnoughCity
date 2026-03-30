/**
 * Tram types matching backend DTOs
 */

export interface TramStop {
  stopId: string;
  line: string;
  name: string;
  lat: number;
  lon: number;
  parkRide: boolean;
  cycleRide: boolean;
}

export interface TramDashboardResponse {
  indicatorType: string;
  totalRecords: number;
  data: TramStop[];
}

export interface TramKpis {
  totalStops: number;
  activeForecastCount: number;
  linesOperating: number;
  avgDueMins: number;
}

export interface TramLiveForecast {
  stopId: string;
  stopName: string;
  line: string;
  direction: string;
  destination: string;
  dueMins: number | null;
  message: string;
  lat: number | null;
  lon: number | null;
}

export interface TramDelay {
  stopId: string;
  stopName: string;
  line: string;
  direction: string;
  destination: string;
  scheduledTime: string;
  dueMins: number;
  delayMins: number;
  estimatedAffectedPassengers: number;
}

export interface TramHourlyDistribution {
  timeLabel: string;
  line: string;
  percentage: number | null;
}

export interface TramAlternativeRoute {
  transportType: "bus" | "rail" | "bike";
  stopId: string;
  stopName: string;
  lat: number;
  lon: number;
  distanceM: number;
  availableBikes?: number;
  capacity?: number;
}
