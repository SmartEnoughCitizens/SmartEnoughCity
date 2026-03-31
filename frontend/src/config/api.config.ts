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
  AUTH_REFRESH: "/api/auth/refresh",
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
  DASHBOARD_TRAM: "/api/v1/dashboard/tram",
  DASHBOARD_CYCLE_AVAILABLE_BIKES: "/api/v1/dashboard/cycle/available-bikes",
  DASHBOARD_CYCLE_AVAILABLE_DOCKS: "/api/v1/dashboard/cycle/available-docks",
  DASHBOARD_BUS_ROUTES: "/api/v1/dashboard/bus/routes",
  DASHBOARD_INDICATOR_TYPES: "/api/v1/dashboard/indicators/types",

  // Car Indicators
  CAR_FUEL_TYPE_STATISTICS: "/api/v1/car/fuel-type-statistics",
  CAR_HIGH_TRAFFIC_POINTS: "/api/v1/car/high-traffic-points",
  CAR_JUNCTION_EMISSIONS: "/api/v1/car/junction-emissions",

  // Bus Indicators
  BUS_KPIS: "/api/v1/bus/kpis",
  BUS_LIVE_VEHICLES: "/api/v1/bus/live-vehicles",
  BUS_ROUTE_UTILIZATION: "/api/v1/bus/route-utilization",
  BUS_SYSTEM_PERFORMANCE: "/api/v1/bus/system-performance",
  BUS_METRICS_REFRESH: "/api/v1/bus/metrics/refresh",
  BUS_COMMON_DELAYS: "/api/v1/bus/common-delays",

  // Train Indicators
  TRAIN_KPIS: "/api/v1/train/kpis",
  TRAIN_LIVE_TRAINS: "/api/v1/train/live-trains",
  TRAIN_SERVICE_STATS: "/api/v1/train/service-stats",
  TRAIN_FREQUENT_DELAYS: "/api/v1/train/frequent-delays",

  // Tram Indicators
  TRAM_KPIS: "/api/v1/tram/kpis",
  TRAM_LIVE_FORECASTS: "/api/v1/tram/live-forecasts",
  TRAM_DELAYS: "/api/v1/tram/delays",
  TRAM_HOURLY_DISTRIBUTION: "/api/v1/tram/hourly-distribution",
  TRAM_STOP_USAGE: "/api/v1/tram/stop-usage",
  TRAM_COMMON_DELAYS: "/api/v1/tram/common-delays",

  // Cycle Metrics (CycleMetricsController)
  CYCLE_STATIONS_LIVE: "/api/v1/cycle/stations/live",
  CYCLE_NETWORK_SUMMARY: "/api/v1/cycle/network/summary",
  CYCLE_RANKINGS_BUSIEST: "/api/v1/cycle/rankings/busiest",
  CYCLE_RANKINGS_UNDERUSED: "/api/v1/cycle/rankings/underused",
  CYCLE_NETWORK_REBALANCING: "/api/v1/cycle/network/rebalancing",

  // Cycle Demand Analysis
  CYCLE_DEMAND_NETWORK_HOURLY: "/api/v1/cycle/demand/network-hourly",
  CYCLE_DEMAND_CLASSIFICATION: "/api/v1/cycle/demand/classification",
  CYCLE_DEMAND_OD_PAIRS: "/api/v1/cycle/demand/od-pairs",
  CYCLE_DEMAND_STATION_HOURLY: "/api/v1/cycle/demand/station-hourly",

  // Recommendation Engine
  RECOMMENDATION_QUERY: "/api/v1/recommendation-engine/indicators/query",
  RECOMMENDATION_GET: (type: string) =>
    `/api/v1/recommendation-engine/indicators/${type}`,

  // Notifications
  NOTIFICATIONS: (userId: string) => `/api/notification/v1/${userId}`,
  NOTIFICATIONS_STREAM: "/api/notification/v1/notifications/stream",

  // Misc (Events + Pedestrians)
  EVENTS: "/api/v1/events",
  PEDESTRIANS_LIVE: "/api/v1/pedestrians/live",

  // Disruptions
  DISRUPTIONS_ACTIVE: "/api/v1/disruptions/active",
  DISRUPTIONS_ALL: "/api/v1/disruptions",
  DISRUPTION_RESOLVE: (id: number) => `/api/v1/disruptions/${id}/resolve`,

  // EV Charging Indicators
  EV_CHARGING_STATIONS: "/api/v1/ev/charging-stations",
  EV_CHARGING_DEMAND: "/api/v1/ev/charging-demand",
  EV_AREAS_GEOJSON: "/api/v1/ev/areas-geojson",
};
