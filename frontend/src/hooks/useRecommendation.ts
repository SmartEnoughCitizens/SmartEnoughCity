/**
 * React Query hooks for recommendation engine
 */

import { useMutation, useQuery } from '@tanstack/react-query';
import { recommendationApi } from '@/api';
import type { RecommendationEngineRequest } from '@/types';

export const RECOMMENDATION_KEYS = {
  indicator: (type: string, limit?: number) =>
    ['recommendation', 'indicator', type, { limit }] as const,
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
 * Get indicator data (GET)
 */
export const useIndicatorData = (
  indicatorType: string,
  limit: number = 100,
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: RECOMMENDATION_KEYS.indicator(indicatorType, limit),
    queryFn: () => recommendationApi.getIndicatorData(indicatorType, limit),
    enabled: !!indicatorType && enabled,
    staleTime: 30000, // 30 seconds
  });
};
