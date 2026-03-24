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
