/**
 * Disruption subscription API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";

export const subscriptionApi = {
  getSubscribedModes: async (): Promise<string[]> => {
    const { data } = await axiosInstance.get<string[]>(
      API_ENDPOINTS.DISRUPTION_SUBSCRIPTIONS,
    );
    return data;
  },

  subscribe: async (mode: string): Promise<void> => {
    await axiosInstance.post(API_ENDPOINTS.DISRUPTION_SUBSCRIPTION_MODE(mode));
  },

  unsubscribe: async (mode: string): Promise<void> => {
    await axiosInstance.delete(
      API_ENDPOINTS.DISRUPTION_SUBSCRIPTION_MODE(mode),
    );
  },
};
