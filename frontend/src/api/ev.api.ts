/**
 * EV Charging API client — proxies calls to Hermes which in turn
 * fetches from the inference engine.
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  EvChargingDemandResponse,
  EvChargingStationsResponse,
  EvAreasGeoJsonResponse,
} from "@/types";

export const evApi = {
  /**
   * GET /api/v1/ev/charging-stations
   * Returns total station count + list of stations with location & charger info.
   */
  getChargingStations: async (): Promise<EvChargingStationsResponse> => {
    const { data } = await axiosInstance.get<EvChargingStationsResponse>(
      API_ENDPOINTS.EV_CHARGING_STATIONS,
    );
    return data;
  },

  /**
   * GET /api/v1/ev/charging-demand
   * Returns summary stats, high-priority areas, and per-area demand breakdown.
   */
  getChargingDemand: async (): Promise<EvChargingDemandResponse> => {
    const { data } = await axiosInstance.get<EvChargingDemandResponse>(
      API_ENDPOINTS.EV_CHARGING_DEMAND,
    );
    return data;
  },

  /**
   * GET /api/v1/ev/areas-geojson
   * Returns GeoJSON for EV coverage areas with proper polygon geometries.
   */
  getAreasGeoJson: async (): Promise<EvAreasGeoJsonResponse> => {
    const { data } = await axiosInstance.get<EvAreasGeoJsonResponse>(
      API_ENDPOINTS.EV_AREAS_GEOJSON,
    );
    return data;
  },
};
