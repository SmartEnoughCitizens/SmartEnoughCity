import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { systemService } from '../services/systemService';
import type { SystemStatus, SystemHealth } from '../types';

// Query keys
export const systemKeys = {
  all: ['system'] as const,
  status: () => [...systemKeys.all, 'status'] as const,
  health: () => [...systemKeys.all, 'health'] as const,
};

// Hook for fetching system status with polling
export const useSystemStatus = (pollingInterval = 30000): UseQueryResult<SystemStatus, Error> => {
  return useQuery({
    queryKey: systemKeys.status(),
    queryFn: () => systemService.getStatus(),
    staleTime: pollingInterval / 2,
    refetchInterval: pollingInterval, // Poll every 30 seconds by default
  });
};

// Hook for fetching system health
export const useSystemHealth = (): UseQueryResult<SystemHealth, Error> => {
  return useQuery({
    queryKey: systemKeys.health(),
    queryFn: () => systemService.getHealth(),
    staleTime: 60000,
  });
};
