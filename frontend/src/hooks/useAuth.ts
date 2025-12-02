import { useMutation, UseMutationResult } from '@tanstack/react-query';
import { authService } from '../services/authService';
import { LoginCredentials, RegisterData, AuthResponse } from '../types';

// Hook for login
export const useLogin = (): UseMutationResult<AuthResponse, Error, LoginCredentials> => {
  return useMutation({
    mutationFn: (credentials: LoginCredentials) => authService.login(credentials),
  });
};

// Hook for registering city manager
export const useRegisterCityManager = (): UseMutationResult<AuthResponse, Error, RegisterData> => {
  return useMutation({
    mutationFn: (userData: RegisterData) => authService.registerCityManager(userData),
  });
};

// Hook for registering service provider admin
export const useRegisterServiceProviderAdmin = (): UseMutationResult<AuthResponse, Error, RegisterData> => {
  return useMutation({
    mutationFn: (userData: RegisterData) => authService.registerServiceProviderAdmin(userData),
  });
};
