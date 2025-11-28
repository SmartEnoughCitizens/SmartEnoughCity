import { UserRole } from '../types';

// API Base URL
export const API_BASE_URL = '/api';

// User Roles
export const ROLES: Record<string, UserRole> = {
  GOVERNMENT_ADMIN: UserRole.GOVERNMENT_ADMIN,
  CITY_MANAGER: UserRole.CITY_MANAGER,
  SERVICE_PROVIDER_ADMIN: UserRole.SERVICE_PROVIDER_ADMIN,
  SERVICE_PROVIDER_USER: UserRole.SERVICE_PROVIDER_USER,
};

// API Endpoints
export const API_ENDPOINTS = {
  // Authentication
  LOGIN: '/auth/login',
  REGISTER_CITY_MANAGER: '/auth/register/city-manager',
  REGISTER_SERVICE_PROVIDER_ADMIN: '/auth/register/service-provider-admin',

  // Users
  USERS: '/users',
  USER_BY_ID: (id: string) => `/users/${id}`,
  SERVICE_PROVIDER_USERS: '/users/service-provider',

  // Indicators
  INDICATORS_CAR: '/indicators/car',
  INDICATORS_CYCLE: '/indicators/cycle',
  INDICATORS_BUS: '/indicators/bus',
  INDICATORS_TRAIN: '/indicators/train',
  INDICATORS_TRAM: '/indicators/tram',
  INDICATORS_PEDESTRIAN: '/indicators/pedestrian',
  INDICATORS_EVENTS: '/indicators/events',
  INDICATORS_CONSTRUCTION: '/indicators/construction',

  // Dashboard
  DASHBOARD_STATS: '/dashboard/stats',
  DASHBOARD_OVERVIEW: '/dashboard/overview',

  // Simulation
  SIMULATIONS: '/simulations',
  SIMULATION_BY_ID: (id: string) => `/simulations/${id}`,
  RUN_SIMULATION: '/simulations/run',

  // Notifications
  NOTIFICATIONS: '/notifications',
  MARK_NOTIFICATION_READ: (id: string) => `/notifications/${id}/read`,
  MARK_ALL_READ: '/notifications/mark-all-read',

  // System Status
  SYSTEM_STATUS: '/system/status',
  SYSTEM_HEALTH: '/system/health',
} as const;

// Transport Modes
export const TRANSPORT_MODES = {
  CAR: 'car',
  CYCLE: 'cycle',
  BUS: 'bus',
  TRAIN: 'train',
  TRAM: 'tram',
  PEDESTRIAN: 'pedestrian',
} as const;

// Notification Types
export const NOTIFICATION_TYPES = {
  INFO: 'INFO',
  WARNING: 'WARNING',
  ERROR: 'ERROR',
  SUCCESS: 'SUCCESS',
} as const;

// Chart Colors
export const CHART_COLORS = {
  primary: '#1976d2',
  secondary: '#dc004e',
  success: '#4caf50',
  warning: '#ff9800',
  error: '#f44336',
  info: '#2196f3',
} as const;

// Map Configuration
export const MAP_CONFIG = {
  center: [51.505, -0.09] as [number, number], // Default center (London)
  zoom: 13,
  minZoom: 10,
  maxZoom: 18,
} as const;
