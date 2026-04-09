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

export interface TramRecommendation {
  id: number;
  indicator: string;
  recommendation: string; // JSON string — array of recommendation objects
  usecase: string;
  simulation: string | null;
  createdAt: string;
  updatedAt: string | null;
  deleted: boolean;
  status: string;
}

/** Parsed shape of a single item inside the recommendation JSON array. */
export interface TramRecommendationItem {
  Name: string;
  Attributes: {
    type:
      | "add_frequency"
      | "reduce_frequency"
      | "partial_run"
      | "rebalance"
      | "monitor";
    line: string;
    time_period: string;
    time_label: string;
    severity: "high" | "medium" | "low" | "very_low";
    description: string;
    [key: string]: unknown; // extra detail fields vary by type
  };
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
