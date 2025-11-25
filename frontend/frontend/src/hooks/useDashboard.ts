import { useQuery, UseQueryResult } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';
import { DashboardStats, DashboardOverview } from '../types';

// Query keys
export const dashboardKeys = {
  all: ['dashboard'] as const,
  stats: () => [...dashboardKeys.all, 'stats'] as const,
  overview: () => [...dashboardKeys.all, 'overview'] as const,
};

// Hook for fetching dashboard stats
export const useDashboardStats = (): UseQueryResult<DashboardStats, Error> => {
  return useQuery({
    queryKey: dashboardKeys.stats(),
    queryFn: () => dashboardService.getStats(),
    staleTime: 30000, // 30 seconds
    refetchInterval: 60000, // Refetch every minute
  });
};

// Hook for fetching dashboard overview
export const useDashboardOverview = (): UseQueryResult<DashboardOverview, Error> => {
  return useQuery({
    queryKey: dashboardKeys.overview(),
    queryFn: () => dashboardService.getOverview(),
    staleTime: 30000,
    refetchInterval: 60000,
  });
};
