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

export interface TramStopUsage {
  stopId: string;
  stopName: string;
  line: string;
  currentHour: number;
  inboundTrips: number;
  outboundTrips: number;
  totalTrips: number;
  estimatedInboundPassengers: number;
  estimatedOutboundPassengers: number;
  estimatedTotalPassengers: number;
  lat: number | null;
  lon: number | null;
}

export interface TramCommonDelay {
  stopId: string;
  stopName: string;
  line: string;
  avgDelayMins: number;
  maxDelayMins: number;
  delayCount: number;
  lat: number | null;
  lon: number | null;
}
