import { useQuery } from "@tanstack/react-query";
import { evApi } from "@/api";

const EV_QUERY_CONFIG = {
  staleTime: Infinity,
  gcTime: 24 * 60 * 60 * 1000,
  refetchOnWindowFocus: false,
  refetchOnMount: false,
} as const;

export const useEvChargingStations = () =>
  useQuery({
    queryKey: ["ev", "charging-stations"],
    queryFn: evApi.getChargingStations,
    ...EV_QUERY_CONFIG,
  });

export const useEvChargingDemand = () =>
  useQuery({
    queryKey: ["ev", "charging-demand"],
    queryFn: evApi.getChargingDemand,
    ...EV_QUERY_CONFIG,
  });

export const useEvAreasGeoJson = () =>
  useQuery({
    queryKey: ["ev", "areas-geojson"],
    queryFn: evApi.getAreasGeoJson,
    ...EV_QUERY_CONFIG,
  });
