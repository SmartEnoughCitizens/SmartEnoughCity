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
