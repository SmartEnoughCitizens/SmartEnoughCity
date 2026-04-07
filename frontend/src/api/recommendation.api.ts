/**
 * Recommendation Engine API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  RecommendationEngineRequest,
  BusTripUpdate,
  CycleStation,
  TrainRecommendation,
} from "@/types";

export const recommendationApi = {
  /**
   * Query indicator data (POST)
   */
  queryIndicators: async (
    request: RecommendationEngineRequest,
  ): Promise<BusTripUpdate[] | CycleStation[]> => {
    const { data } = await axiosInstance.post(
      API_ENDPOINTS.RECOMMENDATION_QUERY,
      request,
    );
    return data;
  },

  /**
   * Get indicator data (GET)
   */
  getIndicatorData: async (
    indicatorType: string,
    limit: number = 100,
  ): Promise<BusTripUpdate[] | CycleStation[]> => {
    const { data } = await axiosInstance.get(
      API_ENDPOINTS.RECOMMENDATION_GET(indicatorType),
      { params: { limit } },
    );
    return data;
  },

  /**
   * Get active recommendations by indicator (GET)
   */
  getRecommendationsByIndicator: async (
    indicator: string,
  ): Promise<TrainRecommendation[]> => {
    const { data } = await axiosInstance.get<TrainRecommendation[]>(
      API_ENDPOINTS.RECOMMENDATION_BY_INDICATOR(indicator),
    );
    return data;
  },
};
