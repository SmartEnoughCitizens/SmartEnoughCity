# API Reference

This document describes the backend APIs consumed by the frontend.

## Configuration

API settings are in `src/config/api.config.ts`:

```typescript
export const API_CONFIG = {
  BASE_URL: import.meta.env.DEV ? 'http://localhost:8080' : '',
  TIMEOUT: 30000,
};
```

## Authentication

### POST `/api/auth/login`

Authenticate a user and receive access tokens.

**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshToken": "string",
  "username": "string",
  "message": "Login successful"
}
```

**Frontend Usage:**
```typescript
import { useLogin } from '@/hooks';

const loginMutation = useLogin();
await loginMutation.mutateAsync({ username, password });
```

### GET `/api/auth/health`

Check authentication service health.

**Response:**
```json
{
  "status": "UP",
  "message": "Auth service is healthy"
}
```

## Dashboard APIs

All dashboard endpoints require authentication via Bearer token.

### GET `/api/v1/dashboard/bus`

Get bus trip update data.

**Query Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| routeId | string | - | Filter by route ID |
| limit | number | 100 | Max records to return |

**Response:**
```json
{
  "indicatorType": "BUS",
  "routeId": "46A",
  "totalRecords": 150,
  "totalRoutes": 25,
  "routes": ["46A", "145", "39A"],
  "data": [
    {
      "id": 1,
      "entityId": "VEH123",
      "tripId": "TRIP456",
      "routeId": "46A",
      "startTime": "08:30:00",
      "startDate": "2024-01-15",
      "stopSequence": "5",
      "stopId": "STOP789",
      "arrivalDelay": 120,
      "departureDelay": 90,
      "scheduleRelationship": "SCHEDULED"
    }
  ],
  "statistics": {
    "routeId": "46A",
    "totalTrips": 150,
    "averageArrivalDelay": 85.5,
    "maxArrivalDelay": 300,
    "minArrivalDelay": -60,
    "averageDepartureDelay": 72.3,
    "maxDepartureDelay": 280,
    "minDepartureDelay": -45
  }
}
```

**Frontend Usage:**
```typescript
import { useBusData } from '@/hooks';

const { data, isLoading, error } = useBusData('46A', 100);
```

### GET `/api/v1/dashboard/bus/routes`

Get list of all available bus routes.

**Response:**
```json
["46A", "145", "39A", "15", "77A"]
```

**Frontend Usage:**
```typescript
import { useBusRoutes } from '@/hooks';

const { data: routes } = useBusRoutes();
```

### GET `/api/v1/dashboard/cycle`

Get cycle station data.

**Query Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| limit | number | 100 | Max records to return |

**Response:**
```json
{
  "indicatorType": "CYCLE",
  "totalRecords": 120,
  "data": [
    {
      "id": 1,
      "stationId": "STATION001",
      "name": "Pearse Street",
      "address": "Pearse Street, Dublin 2",
      "capacity": 40,
      "numBikesAvailable": 12,
      "numDocksAvailable": 28,
      "isInstalled": true,
      "isRenting": true,
      "isReturning": true,
      "lastReported": 1705312800,
      "lastReportedDt": "2024-01-15T10:00:00Z",
      "lat": 53.3438,
      "lon": -6.2505,
      "occupancyRate": 30.0
    }
  ],
  "statistics": {
    "totalStations": 120,
    "averageBikesAvailable": 8.5,
    "totalBikesAvailable": 1020,
    "maxBikesAtStation": 35,
    "averageDocksAvailable": 12.3,
    "totalDocksAvailable": 1476,
    "averageOccupancyRate": 41.2,
    "stationsRenting": 115,
    "stationsReturning": 118
  }
}
```

**Frontend Usage:**
```typescript
import { useCycleData } from '@/hooks';

const { data, isLoading } = useCycleData(200);
```

### GET `/api/v1/dashboard/cycle/available-bikes`

Get stations with bikes available for rent.

**Response:** Array of `CycleStation` objects (same structure as above).

### GET `/api/v1/dashboard/cycle/available-docks`

Get stations with docks available for returns.

**Response:** Array of `CycleStation` objects.

### GET `/api/v1/dashboard/indicators/types`

Get available indicator types.

**Response:**
```json
["bus", "cycle", "luas", "train"]
```

## Notifications

### GET `/notification/v1/{userId}`

Get notifications for a user.

**Path Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| userId | string | Username |

**Response:**
```json
{
  "userId": "john.doe",
  "notifications": [
    {
      "id": "notif-001",
      "type": "ROUTE_RECOMMENDATION",
      "message": "Route 46A experiencing delays. Consider taking 145.",
      "priority": "HIGH",
      "timestamp": "2024-01-15T10:30:00Z",
      "metadata": {
        "affectedRoute": "46A",
        "alternativeRoute": "145"
      },
      "read": false
    }
  ],
  "totalCount": 5
}
```

**Notification Types:**
- `ROUTE_RECOMMENDATION` - Suggested alternative routes
- `ALERT` - Service alerts and disruptions
- `UPDATE` - General updates
- `SYSTEM` - System notifications

**Priority Levels:**
- `LOW` - Informational
- `MEDIUM` - Standard notifications
- `HIGH` - Important updates
- `URGENT` - Critical alerts

**Frontend Usage:**
```typescript
import { useUserNotifications } from '@/hooks';

const { data } = useUserNotifications(username);
```

## Recommendation Engine

### POST `/api/v1/recommendation-engine/indicators/query`

Query indicator data with filters.

**Request:**
```json
{
  "indicatorType": "bus",
  "startDate": "2024-01-01",
  "endDate": "2024-01-15",
  "limit": 100,
  "aggregationType": "daily"
}
```

**Response:** Array of `BusTripUpdate` or `CycleStation` depending on indicator type.

### GET `/api/v1/recommendation-engine/indicators/{type}`

Get indicator data by type.

**Path Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| type | string | Indicator type (bus, cycle, etc.) |

**Query Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| limit | number | 100 | Max records |

## Error Responses

All endpoints return errors in this format:

```json
{
  "error": "Error message description"
}
```

**HTTP Status Codes:**
| Code | Meaning |
|------|---------|
| 400 | Bad Request - Invalid parameters |
| 401 | Unauthorized - Invalid or expired token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 500 | Internal Server Error |

## Axios Configuration

The frontend uses a configured Axios instance (`src/utils/axios.ts`) that:

1. **Adds auth header** to all requests:
```typescript
config.headers.Authorization = `Bearer ${token}`;
```

2. **Handles 401 errors** by clearing auth and redirecting:
```typescript
if (error.response?.status === 401) {
  localStorage.clear();
  window.location.href = '/login';
}
```

## Adding New API Endpoints

1. **Add endpoint to config:**
```typescript
// src/config/api.config.ts
export const API_ENDPOINTS = {
  // ...existing
  NEW_ENDPOINT: '/api/v1/new-feature',
};
```

2. **Create API client:**
```typescript
// src/api/newFeature.api.ts
export const newFeatureApi = {
  getData: async () => {
    const { data } = await axiosInstance.get(API_ENDPOINTS.NEW_ENDPOINT);
    return data;
  },
};
```

3. **Export from barrel:**
```typescript
// src/api/index.ts
export * from './newFeature.api';
```

4. **Create hook:**
```typescript
// src/hooks/useNewFeature.ts
export const useNewFeature = () => {
  return useQuery({
    queryKey: ['newFeature'],
    queryFn: () => newFeatureApi.getData(),
  });
};
```

## Mock Data (Development)

For local development without backend, you can add MSW (Mock Service Worker) or use React Query's placeholder data:

```typescript
const { data } = useQuery({
  queryKey: ['example'],
  queryFn: fetchData,
  placeholderData: mockData, // Shows while loading
});
```
