# Architecture Guide

This document explains the frontend architecture, design patterns, and data flow.

## Overview

The Hermes frontend follows a layered architecture separating concerns into distinct modules:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────────┐ │
│  │  Pages  │  │ Layouts │  │Components│  │     Router      │ │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────────┬────────┘ │
└───────┼────────────┼────────────┼────────────────┼──────────┘
        │            │            │                │
┌───────┼────────────┼────────────┼────────────────┼──────────┐
│       ▼            ▼            ▼                ▼          │
│                    State Management Layer                    │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │    Redux Store      │    │       React Query           │ │
│  │  (UI State, Auth)   │    │    (Server State)           │ │
│  └──────────┬──────────┘    └──────────────┬──────────────┘ │
└─────────────┼───────────────────────────────┼───────────────┘
              │                               │
┌─────────────┼───────────────────────────────┼───────────────┐
│             ▼                               ▼               │
│                      Data Layer                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────┐  │
│  │   Hooks     │───▶│    API      │───▶│     Axios       │  │
│  └─────────────┘    └─────────────┘    └────────┬────────┘  │
└─────────────────────────────────────────────────┼───────────┘
                                                  │
                                                  ▼
                                          ┌──────────────┐
                                          │ Backend APIs │
                                          └──────────────┘
```

## State Management Strategy

We use a **dual state management** approach:

### Redux Toolkit (Client State)
Used for UI state that doesn't come from the server:
- Authentication status (`authSlice`)
- Theme preferences (`uiSlice`)
- Sidebar open/closed state
- Notification badge count

```typescript
// Example: Toggle theme
dispatch(toggleTheme());

// Example: Read auth state
const { isAuthenticated, username } = useAppSelector(state => state.auth);
```

### React Query (Server State)
Used for all data fetched from APIs:
- Bus trip data
- Cycle station data
- Notifications
- User recommendations

```typescript
// Example: Fetch bus data with caching
const { data, isLoading, error } = useBusData(routeId, limit);
```

**Why this split?**
- Server state has different concerns (caching, refetching, staleness)
- React Query handles these automatically
- Redux remains simple and focused on UI state

## Data Flow

### 1. API Layer (`src/api/`)

Each API module exports functions that make HTTP requests:

```typescript
// src/api/dashboard.api.ts
export const dashboardApi = {
  getBusData: async (params) => {
    const { data } = await axiosInstance.get('/api/v1/dashboard/bus', { params });
    return data;
  },
};
```

### 2. Hooks Layer (`src/hooks/`)

Custom hooks wrap React Query, providing:
- Type-safe query keys
- Configured caching/refetch behavior
- Reusable data fetching logic

```typescript
// src/hooks/useDashboard.ts
export const useBusData = (routeId?: string, limit = 100) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.bus(routeId, limit),
    queryFn: () => dashboardApi.getBusData({ routeId, limit }),
    staleTime: 30000,
  });
};
```

### 3. Component Layer

Components consume hooks and render data:

```typescript
// src/pages/BusDashboard.tsx
export const BusDashboard = () => {
  const { data, isLoading, error } = useBusData(selectedRoute, 100);
  
  if (isLoading) return <CircularProgress />;
  if (error) return <Alert severity="error">Failed to load</Alert>;
  
  return <BusTripTable trips={data?.data || []} />;
};
```

## Component Organization

### Pages (`src/pages/`)
Top-level route components. Each page:
- Fetches its own data via hooks
- Composes layout and child components
- Handles page-level loading/error states

### Layouts (`src/layouts/`)
Structural wrappers providing:
- Navigation (sidebar, header)
- Authentication boundary
- Theme application

### Components (`src/components/`)
Reusable UI pieces organized by domain:

```
components/
├── auth/           # Login, protected routes
│   ├── LoginForm.tsx
│   └── ProtectedRoute.tsx
├── charts/         # Data visualization
│   ├── DelayChart.tsx
│   └── CycleStatsChart.tsx
├── map/            # Geographic displays
│   └── CycleStationMap.tsx
└── tables/         # Data tables
    ├── BusTripTable.tsx
    └── CycleStationTable.tsx
```

## Routing Architecture

Routes are defined in `src/router/index.tsx` using React Router 7:

```typescript
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/dashboard',
    element: <ProtectedDashboardLayout />,  // Wraps all children
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'bus', element: <BusDashboard /> },
      { path: 'cycle', element: <CycleDashboard /> },
      { path: 'notifications', element: <NotificationsPage /> },
    ],
  },
]);
```

**Key patterns:**
- Nested routes share the `DashboardLayout`
- All dashboard routes are protected via `ProtectedRoute`
- Pages are lazy-loaded for code splitting

## Authentication Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  LoginForm  │────▶│  useLogin   │────▶│  authApi    │
└─────────────┘     └──────┬──────┘     └──────┬──────┘
                          │                    │
                          ▼                    ▼
                   ┌─────────────┐      ┌─────────────┐
                   │ Redux Store │      │  Backend    │
                   │ (setAuth)   │      │  /api/auth  │
                   └──────┬──────┘      └─────────────┘
                          │
                          ▼
                   ┌─────────────┐
                   │localStorage │
                   │ (tokens)    │
                   └─────────────┘
```

1. User submits credentials via `LoginForm`
2. `useLogin` mutation calls `authApi.login()`
3. On success:
   - Tokens stored in localStorage
   - Redux state updated via `setAuthenticated`
   - User redirected to dashboard

## Theming

The app supports light/dark themes via MUI:

```typescript
// src/theme/index.ts
export const getTheme = (mode: 'light' | 'dark') => {
  return mode === 'light' ? lightTheme : darkTheme;
};

// src/App.tsx - Theme applied based on Redux state
const theme = useAppSelector((state) => state.ui.theme);
<ThemeProvider theme={getTheme(theme)}>
```

Theme preference is persisted to localStorage via the `uiSlice`.

## Type System

All types are centralized in `src/types/`:

```
types/
├── index.ts              # Re-exports all types
├── auth.types.ts         # Login/logout DTOs
├── bus.types.ts          # Bus trip data
├── cycle.types.ts        # Cycle station data
├── notification.types.ts # User notifications
├── recommendation.types.ts
└── simulation.types.ts
```

Types mirror backend DTOs for type-safe API integration.

## Error Handling

### API Errors
Axios interceptors handle global error cases:

```typescript
// src/utils/axios.ts
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Clear auth and redirect to login
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### Component-Level Errors
React Query provides error states per query:

```typescript
const { error } = useBusData();
if (error) return <Alert severity="error">Failed to load</Alert>;
```

## Performance Considerations

1. **Code Splitting**: Pages are lazy-loaded via `React.lazy()`
2. **Query Caching**: React Query caches responses (30s stale time)
3. **Memoization**: Heavy computations use `useMemo`/`useCallback`
4. **Virtualization**: Large tables should implement row virtualization (TODO)
