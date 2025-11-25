import { useQuery, useMutation, useQueryClient, UseQueryResult, UseMutationResult } from '@tanstack/react-query';
import { userService } from '../services/userService';
import { UserProfile, CreateUserRequest, UpdateUserRequest } from '../types';

// Query keys
export const userKeys = {
  all: ['users'] as const,
  lists: () => [...userKeys.all, 'list'] as const,
  list: (filters?: unknown) => [...userKeys.lists(), filters] as const,
  details: () => [...userKeys.all, 'detail'] as const,
  detail: (id: string) => [...userKeys.details(), id] as const,
  serviceProviders: () => [...userKeys.all, 'service-providers'] as const,
};

// Hook for fetching all users
export const useUsers = (): UseQueryResult<UserProfile[], Error> => {
  return useQuery({
    queryKey: userKeys.lists(),
    queryFn: () => userService.getAllUsers(),
    staleTime: 60000,
  });
};

// Hook for fetching user by ID
export const useUser = (id: string): UseQueryResult<UserProfile, Error> => {
  return useQuery({
    queryKey: userKeys.detail(id),
    queryFn: () => userService.getUserById(id),
    enabled: !!id,
    staleTime: 60000,
  });
};

// Hook for fetching service provider users
export const useServiceProviderUsers = (): UseQueryResult<UserProfile[], Error> => {
  return useQuery({
    queryKey: userKeys.serviceProviders(),
    queryFn: () => userService.getServiceProviderUsers(),
    staleTime: 60000,
  });
};

// Hook for creating service provider user
export const useCreateUser = (): UseMutationResult<UserProfile, Error, CreateUserRequest> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userData: CreateUserRequest) => userService.createServiceProviderUser(userData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
      queryClient.invalidateQueries({ queryKey: userKeys.serviceProviders() });
    },
  });
};

// Hook for updating user
export const useUpdateUser = (): UseMutationResult<UserProfile, Error, { id: string; data: UpdateUserRequest }> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) => userService.updateUser(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
      queryClient.invalidateQueries({ queryKey: userKeys.detail(variables.id) });
    },
  });
};

// Hook for deleting user
export const useDeleteUser = (): UseMutationResult<void, Error, string> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => userService.deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
};
