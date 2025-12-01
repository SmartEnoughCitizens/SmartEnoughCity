export interface User {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  firstName?: string;
  lastName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export enum UserRole {
  GOVERNMENT_ADMIN = 'GOVERNMENT_ADMIN',
  CITY_MANAGER = 'CITY_MANAGER',
  SERVICE_PROVIDER_ADMIN = 'SERVICE_PROVIDER_ADMIN',
  SERVICE_PROVIDER_USER = 'SERVICE_PROVIDER_USER',
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface RegisterData {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  userType: UserRole;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}
