import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { subscriptionApi } from "@/api";

export const SUBSCRIPTION_KEYS = {
  all: ["disruption-subscriptions"] as const,
};

export const useDisruptionSubscriptions = () => {
  return useQuery<string[]>({
    queryKey: SUBSCRIPTION_KEYS.all,
    queryFn: () => subscriptionApi.getSubscribedModes(),
    staleTime: 60_000,
  });
};

export const useSubscribeToMode = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (mode: string) => subscriptionApi.subscribe(mode),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SUBSCRIPTION_KEYS.all });
    },
  });
};

export const useUnsubscribeFromMode = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (mode: string) => subscriptionApi.unsubscribe(mode),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SUBSCRIPTION_KEYS.all });
    },
  });
};
