/**
 * API configuration
 */

export const API_CONFIG = {
  BASE_URL: import.meta.env.DEV ? "http://localhost:8080" : "",
  TIMEOUT: 30_000,
  HEADERS: {
    "Content-Type": "application/json",
  },
};

export const API_ENDPOINTS = {
  // Auth
  AUTH_LOGIN: "/api/auth/login",
  AUTH_HEALTH: "/api/auth/health",

  // Public
  PUBLIC_HEALTH: "/api/public/health",

  // User Management
  TRAINS: "/api/trains",
  BUSES: "/api/buses",

  // Dashboard
  DASHBOARD_BUS: "/api/v1/dashboard/bus",
  DASHBOARD_CYCLE: "/api/v1/dashboard/cycle",
  DASHBOARD_CYCLE_AVAILABLE_BIKES: "/api/v1/dashboard/cycle/available-bikes",
  DASHBOARD_CYCLE_AVAILABLE_DOCKS: "/api/v1/dashboard/cycle/available-docks",
  DASHBOARD_BUS_ROUTES: "/api/v1/dashboard/bus/routes",
  DASHBOARD_INDICATOR_TYPES: "/api/v1/dashboard/indicators/types",

  // Bus Indicators
  BUS_KPIS: "/api/v1/bus/kpis",
  BUS_LIVE_VEHICLES: "/api/v1/bus/live-vehicles",
  BUS_ROUTE_UTILIZATION: "/api/v1/bus/route-utilization",
  BUS_SYSTEM_PERFORMANCE: "/api/v1/bus/system-performance",
  BUS_METRICS_REFRESH: "/api/v1/bus/metrics/refresh",

  // Recommendation Engine
  RECOMMENDATION_QUERY: "/api/v1/recommendation-engine/indicators/query",
  RECOMMENDATION_GET: (type: string) =>
    `/api/v1/recommendation-engine/indicators/${type}`,

  // Notifications
  NOTIFICATIONS: (userId: string) => `/api/notification/v1/${userId}`,
  NOTIFICATIONS_STREAM: "/api/notification/v1/notifications/stream",
};
