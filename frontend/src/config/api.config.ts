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
  AUTH_FORGOT_PASSWORD: "/api/auth/forgot-password",
  AUTH_RESET_PASSWORD: "/api/auth/reset-password",

  // Public
  PUBLIC_HEALTH: "/api/public/health",

  // User Management
  USER_REGISTER: "/api/usermanagement/register",
  USER_LIST: "/api/usermanagement/users",
  USER_DELETE: "/api/usermanagement/delete",
  USER_PROFILE: "/api/usermanagement/profile",
  USER_PASSWORD: "/api/usermanagement/password",
  TRAINS: "/api/trains",
  BUSES: "/api/buses",

  // Dashboard
  DASHBOARD_BUS: "/api/v1/dashboard/bus",
  DASHBOARD_CYCLE: "/api/v1/dashboard/cycle",
  DASHBOARD_TRAIN: "/api/v1/dashboard/train",
  DASHBOARD_CYCLE_AVAILABLE_BIKES: "/api/v1/dashboard/cycle/available-bikes",
  DASHBOARD_CYCLE_AVAILABLE_DOCKS: "/api/v1/dashboard/cycle/available-docks",
  DASHBOARD_BUS_ROUTES: "/api/v1/dashboard/bus/routes",
  DASHBOARD_INDICATOR_TYPES: "/api/v1/dashboard/indicators/types",

  // Car Indicators
  CAR_FUEL_TYPE_STATISTICS: "/api/v1/car/fuel-type-statistics",
  CAR_HIGH_TRAFFIC_POINTS: "/api/v1/car/high-traffic-points",

  // Bus Indicators
  BUS_KPIS: "/api/v1/bus/kpis",
  BUS_LIVE_VEHICLES: "/api/v1/bus/live-vehicles",
  BUS_ROUTE_UTILIZATION: "/api/v1/bus/route-utilization",
  BUS_SYSTEM_PERFORMANCE: "/api/v1/bus/system-performance",
  BUS_METRICS_REFRESH: "/api/v1/bus/metrics/refresh",

  // Train Indicators
  TRAIN_KPIS: "/api/v1/train/kpis",
  TRAIN_LIVE_TRAINS: "/api/v1/train/live-trains",
  TRAIN_SERVICE_STATS: "/api/v1/train/service-stats",

  // Recommendation Engine
  RECOMMENDATION_QUERY: "/api/v1/recommendation-engine/indicators/query",
  RECOMMENDATION_GET: (type: string) =>
    `/api/v1/recommendation-engine/indicators/${type}`,

  // Notifications
  NOTIFICATIONS: (userId: string) => `/api/notification/v1/${userId}`,
  NOTIFICATIONS_STREAM: "/api/notification/v1/notifications/stream",
  /** DELETE endpoint — requires write permission */
  NOTIFICATION_DISMISS: (userId: string, notificationId: string) =>
    `/api/notification/v1/${userId}/${notificationId}`,
};
