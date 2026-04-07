/**
 * React Query hooks for recommendation engine
 */

import { useMutation, useQuery } from "@tanstack/react-query";
import { recommendationApi } from "@/api";
import type { RecommendationEngineRequest } from "@/types";

export const RECOMMENDATION_KEYS = {
  indicator: (type: string, limit?: number) =>
    ["recommendation", "indicator", type, { limit }] as const,
  byIndicator: (indicator: string) =>
    ["recommendation", "by-indicator", indicator] as const,
};

/**
 * Query indicators (POST)
 */
export const useQueryIndicators = () => {
  return useMutation({
    mutationFn: (request: RecommendationEngineRequest) =>
      recommendationApi.queryIndicators(request),
  });
};

/**
 * Get active recommendations by indicator
 */
export const useTrainRecommendations = (enabled: boolean = true) => {
  return useQuery({
    queryKey: RECOMMENDATION_KEYS.byIndicator("train"),
    queryFn: () => recommendationApi.getRecommendationsByIndicator("train"),
    enabled,
    staleTime: 60_000,
  });
};

/**
 * Get indicator data (GET)
 */
export const useIndicatorData = (
  indicatorType: string,
  limit: number = 100,
  enabled: boolean = true,
) => {
  return useQuery({
    queryKey: RECOMMENDATION_KEYS.indicator(indicatorType, limit),
    queryFn: () => recommendationApi.getIndicatorData(indicatorType, limit),
    enabled: !!indicatorType && enabled,
    staleTime: 30_000, // 30 seconds
  });
};
