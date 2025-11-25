import { useQuery, UseQueryResult } from '@tanstack/react-query';
import { indicatorService } from '../services/indicatorService';
import {
  TransportIndicatorData,
  EventIndicator,
  ConstructionIndicator,
  IndicatorFilters,
  TransportMode
} from '../types';

// Query keys
export const indicatorKeys = {
  all: ['indicators'] as const,
  transport: (mode: TransportMode, filters?: IndicatorFilters) =>
    [...indicatorKeys.all, 'transport', mode, filters] as const,
  events: (filters?: IndicatorFilters) =>
    [...indicatorKeys.all, 'events', filters] as const,
  construction: (filters?: IndicatorFilters) =>
    [...indicatorKeys.all, 'construction', filters] as const,
};

// Hook for fetching car indicators
export const useCarIndicators = (filters?: IndicatorFilters): UseQueryResult<TransportIndicatorData, Error> => {
  return useQuery({
    queryKey: indicatorKeys.transport('car', filters),
    queryFn: () => indicatorService.getCarIndicators(filters),
    staleTime: 60000,
  });
};

// Hook for fetching cycle indicators
export const useCycleIndicators = (filters?: IndicatorFilters): UseQueryResult<TransportIndicatorData, Error> => {
  return useQuery({
    queryKey: indicatorKeys.transport('cycle', filters),
    queryFn: () => indicatorService.getCycleIndicators(filters),
    staleTime: 60000,
  });
};

// Hook for fetching bus indicators
export const useBusIndicators = (filters?: IndicatorFilters): UseQueryResult<TransportIndicatorData, Error> => {
  return useQuery({
    queryKey: indicatorKeys.transport('bus', filters),
    queryFn: () => indicatorService.getBusIndicators(filters),
    staleTime: 60000,
  });
};

// Hook for fetching train indicators
export const useTrainIndicators = (filters?: IndicatorFilters): UseQueryResult<TransportIndicatorData, Error> => {
  return useQuery({
    queryKey: indicatorKeys.transport('train', filters),
    queryFn: () => indicatorService.getTrainIndicators(filters),
    staleTime: 60000,
  });
};

// Hook for fetching tram indicators
export const useTramIndicators = (filters?: IndicatorFilters): UseQueryResult<TransportIndicatorData, Error> => {
  return useQuery({
    queryKey: indicatorKeys.transport('tram', filters),
    queryFn: () => indicatorService.getTramIndicators(filters),
    staleTime: 60000,
  });
};

// Hook for fetching pedestrian indicators
export const usePedestrianIndicators = (filters?: IndicatorFilters): UseQueryResult<TransportIndicatorData, Error> => {
  return useQuery({
    queryKey: indicatorKeys.transport('pedestrian', filters),
    queryFn: () => indicatorService.getPedestrianIndicators(filters),
    staleTime: 60000,
  });
};

// Hook for fetching events indicators
export const useEventsIndicators = (filters?: IndicatorFilters): UseQueryResult<EventIndicator[], Error> => {
  return useQuery({
    queryKey: indicatorKeys.events(filters),
    queryFn: () => indicatorService.getEventsIndicators(filters),
    staleTime: 60000,
  });
};

// Hook for fetching construction indicators
export const useConstructionIndicators = (filters?: IndicatorFilters): UseQueryResult<ConstructionIndicator[], Error> => {
  return useQuery({
    queryKey: indicatorKeys.construction(filters),
    queryFn: () => indicatorService.getConstructionIndicators(filters),
    staleTime: 60000,
  });
};
