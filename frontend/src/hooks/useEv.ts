/**
 * React Query hooks for EV charging data
 * Configured with aggressive caching to prevent repeated network calls
 */

import { useQuery } from "@tanstack/react-query";
import { evApi } from "@/api";

export const useEvChargingStations = () =>
  useQuery({
    queryKey: ["ev", "charging-stations"],
    queryFn: evApi.getChargingStations,
    staleTime: 5 * 60 * 1000, // 5 minutes - data stays fresh
    gcTime: 30 * 60 * 1000, // 30 minutes - cache time (formerly cacheTime)
    refetchOnWindowFocus: false, // Don't refetch when window regains focus
    refetchOnMount: false, // Don't refetch on component mount if data exists
  });

export const useEvChargingDemand = () =>
  useQuery({
    queryKey: ["ev", "charging-demand"],
    queryFn: evApi.getChargingDemand,
    staleTime: 5 * 60 * 1000, // 5 minutes - data stays fresh
    gcTime: 30 * 60 * 1000, // 30 minutes - cache time (formerly cacheTime)
    refetchOnWindowFocus: false, // Don't refetch when window regains focus
    refetchOnMount: false, // Don't refetch on component mount if data exists
  });

export const useEvAreasGeoJson = () =>
  useQuery({
    queryKey: ["ev", "areas-geojson"],
    queryFn: evApi.getAreasGeoJson,
    staleTime: 5 * 60 * 1000, // 5 minutes - data stays fresh
    gcTime: 30 * 60 * 1000, // 30 minutes - cache time (formerly cacheTime)
    refetchOnWindowFocus: false, // Don't refetch when window regains focus
    refetchOnMount: false, // Don't refetch on component mount if data exists
  });
