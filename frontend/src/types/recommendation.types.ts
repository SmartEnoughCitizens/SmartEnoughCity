/**
 * Recommendation engine types matching backend DTOs
 */

export interface RecommendationEngineRequest {
  indicatorType: string;
  startDate?: string;
  endDate?: string;
  limit?: number;
  aggregationType?: string;
}

export type IndicatorType = "bus" | "cycle" | "luas" | "train";
