/**
 * Car indicator types matching backend DTOs
 */

export interface CarFuelTypeStat {
  fuelType: string;
  count: number;
}

export interface HighTrafficPoint {
  siteId: number;
  lat: number;
  lon: number;
  avgVolume: number;
  dayType: string;
  timeSlot: string;
}

export interface JunctionEmission {
  siteId: number;
  lat: number;
  lon: number;
  dayType: string;
  timeSlot: string;
  carVolume: number;
  lcvVolume: number;
  busVolume: number;
  hgvVolume: number;
  motorcycleVolume: number;
  totalEmissionG: number;
}

export interface TrafficRouteWaypoint {
  lat: number;
  lon: number;
}

export interface TrafficAlternativeRoute {
  routeId: string;
  label: string;
  summary: string;
  color: string;
  estimatedTimeSavingsMinutes: number;
  estimatedTravelTimeMinutes: number;
  distanceKm: number;
  path: TrafficRouteWaypoint[];
}

export interface TrafficRecommendation {
  recommendationId: string;
  siteId: number;
  siteLat: number;
  siteLon: number;
  title: string;
  summary: string;
  dayType: string;
  timeSlot: string;
  averageVolume: number;
  congestionLevel: string;
  confidenceScore: number;
  recommendedAction: string;
  generatedAt: string;
  alternativeRoutes: TrafficAlternativeRoute[];
}
