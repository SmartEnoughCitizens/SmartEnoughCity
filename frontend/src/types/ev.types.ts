/**
 * EV charging indicator types — matches Hermes backend DTOs exactly.
 * Snake_case field names mirror the backend JSON contract.
 */

export interface EvStation {
  address: string;
  county: string;
  latitude: number;
  longitude: number;
  charger_count: number;
  open_hours: string;
}

export interface EvChargingStationsResponse {
  total_stations: number;
  stations: EvStation[];
}

export interface EvAreaDemand {
  area: string;
  registered_evs: number;
  charging_demand: number;
  home_charge_percentage: number;
  charge_frequency: number;
}

export interface EvChargingDemandResponse {
  summary: Record<string, unknown>;
  high_priority_areas: string[];
  areas: EvAreaDemand[];
}

export interface EvAreaGeoJsonFeature {
  type: "Feature";
  geometry: {
    type: "Polygon" | "MultiPolygon";
    coordinates: number[][][] | number[][][][];
  };
  properties: {
    ED_ENGLISH: string;
    COUNTY_ENGLISH: string;
    display_name: string;
    charging_demand?: number;
    registered_ev?: number;
  };
}

export interface EvAreasGeoJsonResponse {
  type: "FeatureCollection";
  features: EvAreaGeoJsonFeature[];
}
