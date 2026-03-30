import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "@/api";

export const TRAFFIC_RECOMMENDATION_KEYS = {
  all: ["car", "traffic-recommendations"] as const,
};

export const useTrafficRecommendations = (enabled = true) => {
  return useQuery({
    queryKey: TRAFFIC_RECOMMENDATION_KEYS.all,
    queryFn: () => dashboardApi.getCarTrafficRecommendations(),
    staleTime: 300_000,
    refetchInterval: 300_000,
    refetchIntervalInBackground: true,
    enabled,
  });
};
