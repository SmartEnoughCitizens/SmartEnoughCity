import { useQuery, useMutation, useQueryClient, UseQueryResult, UseMutationResult } from '@tanstack/react-query';
import { simulationService } from '../services/simulationService';
import { Simulation, CreateSimulationRequest } from '../types';

// Query keys
export const simulationKeys = {
  all: ['simulations'] as const,
  lists: () => [...simulationKeys.all, 'list'] as const,
  details: () => [...simulationKeys.all, 'detail'] as const,
  detail: (id: string) => [...simulationKeys.details(), id] as const,
};

// Hook for fetching all simulations
export const useSimulations = (): UseQueryResult<Simulation[], Error> => {
  return useQuery({
    queryKey: simulationKeys.lists(),
    queryFn: () => simulationService.getAllSimulations(),
    staleTime: 60000,
  });
};

// Hook for fetching simulation by ID
export const useSimulation = (id: string): UseQueryResult<Simulation, Error> => {
  return useQuery({
    queryKey: simulationKeys.detail(id),
    queryFn: () => simulationService.getSimulationById(id),
    enabled: !!id,
    staleTime: 60000,
  });
};

// Hook for running simulation
export const useRunSimulation = (): UseMutationResult<Simulation, Error, CreateSimulationRequest> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (simulationData: CreateSimulationRequest) => simulationService.runSimulation(simulationData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: simulationKeys.lists() });
    },
  });
};

// Hook for deleting simulation
export const useDeleteSimulation = (): UseMutationResult<void, Error, string> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => simulationService.deleteSimulation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: simulationKeys.lists() });
    },
  });
};
