# Contributing Guide

Welcome! This guide will help you contribute effectively to the Hermes frontend.

## Development Setup

### Prerequisites

- Node.js 18+ 
- npm 9+
- Backend services running (see [API docs](./API.md))

### Installation

```bash
# Clone the repository
git clone <repo-url>
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

## Code Style

### TypeScript

- Enable strict mode (already configured)
- Always define explicit return types for functions
- Use interfaces for object shapes, types for unions/primitives
- Prefer `type` imports: `import type { User } from '@/types'`

```typescript
// ✅ Good
interface UserProps {
  user: User;
  onSelect: (id: string) => void;
}

// ❌ Avoid
const UserCard = (props: any) => { ... }
```

### React Components

- Use functional components with hooks
- One component per file
- Name files same as component: `BusTripTable.tsx` exports `BusTripTable`

```typescript
// ✅ Good
interface BusTripTableProps {
  trips: BusTripUpdate[];
  maxRows?: number;
}

export const BusTripTable = ({ trips, maxRows = 10 }: BusTripTableProps) => {
  // ...
};
```

### File Organization

```
components/
└── feature/
    └── FeatureName.tsx    # Component + types in same file

// For complex components with many helpers:
components/
└── feature/
    ├── index.ts           # Re-exports
    ├── FeatureName.tsx    # Main component
    ├── FeatureName.types.ts
    └── FeatureName.utils.ts
```

## Adding a New Feature

### 1. Define Types

Start by adding types to `src/types/`:

```typescript
// src/types/train.types.ts
export interface TrainUpdate {
  id: number;
  trainId: string;
  stationId: string;
  delay: number;
}

// src/types/index.ts - Add export
export * from './train.types';
```

### 2. Create API Client

Add API functions to `src/api/`:

```typescript
// src/api/train.api.ts
import { axiosInstance } from '@/utils/axios';
import { API_ENDPOINTS } from '@/config/api.config';
import type { TrainUpdate } from '@/types';

export const trainApi = {
  getTrainData: async (limit = 100): Promise<TrainUpdate[]> => {
    const { data } = await axiosInstance.get(API_ENDPOINTS.TRAINS, {
      params: { limit },
    });
    return data;
  },
};

// src/api/index.ts - Add export
export * from './train.api';
```

### 3. Create Custom Hook

Wrap React Query in a reusable hook:

```typescript
// src/hooks/useTrain.ts
import { useQuery } from '@tanstack/react-query';
import { trainApi } from '@/api';

export const TRAIN_KEYS = {
  all: (limit?: number) => ['trains', { limit }] as const,
};

export const useTrainData = (limit = 100) => {
  return useQuery({
    queryKey: TRAIN_KEYS.all(limit),
    queryFn: () => trainApi.getTrainData(limit),
    staleTime: 30000,
  });
};

// src/hooks/index.ts - Add export
export * from './useTrain';
```

### 4. Build Components

Create UI components in `src/components/`:

```typescript
// src/components/tables/TrainTable.tsx
import { Paper, Table, TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import type { TrainUpdate } from '@/types';

interface TrainTableProps {
  trains: TrainUpdate[];
  maxRows?: number;
}

export const TrainTable = ({ trains, maxRows = 10 }: TrainTableProps) => {
  const displayTrains = trains.slice(0, maxRows);
  
  return (
    <Paper>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Train ID</TableCell>
            <TableCell>Station</TableCell>
            <TableCell>Delay</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {displayTrains.map((train) => (
            <TableRow key={train.id}>
              <TableCell>{train.trainId}</TableCell>
              <TableCell>{train.stationId}</TableCell>
              <TableCell>{train.delay}s</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
};
```

### 5. Create Page

Add a new page in `src/pages/`:

```typescript
// src/pages/TrainDashboard.tsx
import { Box, Typography, CircularProgress, Alert } from '@mui/material';
import { useTrainData } from '@/hooks';
import { TrainTable } from '@/components/tables/TrainTable';

export const TrainDashboard = () => {
  const { data, isLoading, error } = useTrainData(100);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">Failed to load train data</Alert>;
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Train Updates
      </Typography>
      <TrainTable trains={data || []} maxRows={50} />
    </Box>
  );
};
```

### 6. Add Route

Register the page in the router:

```typescript
// src/router/index.tsx
const TrainDashboard = lazy(() =>
  import('@/pages/TrainDashboard').then((m) => ({ default: m.TrainDashboard }))
);

// In router config, add to dashboard children:
{ path: 'trains', element: <TrainDashboard /> },
```

### 7. Add Navigation

Update the sidebar in `DashboardLayout.tsx`:

```typescript
const menuItems = [
  // ... existing items
  { text: 'Train Data', icon: <TrainIcon />, path: '/dashboard/trains' },
];
```

## State Management Guidelines

### When to Use Redux

Use Redux (`src/store/slices/`) for:
- UI state (theme, sidebar, modals)
- Auth state (tokens, user info)
- Cross-component state that isn't from server

```typescript
// Creating a new slice
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface MyState {
  value: string;
}

const mySlice = createSlice({
  name: 'myFeature',
  initialState: { value: '' } as MyState,
  reducers: {
    setValue: (state, action: PayloadAction<string>) => {
      state.value = action.payload;
    },
  },
});

export const { setValue } = mySlice.actions;
export default mySlice.reducer;
```

### When to Use React Query

Use React Query (via custom hooks) for:
- All server data (API responses)
- Data that needs caching/refetching
- Mutations (POST, PUT, DELETE)

```typescript
// Mutations example
export const useCreateTrain = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: CreateTrainRequest) => trainApi.createTrain(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trains'] });
    },
  });
};
```

## Testing (TODO)

We plan to add:
- Unit tests with Vitest
- Component tests with React Testing Library
- E2E tests with Playwright

## Common Patterns

### Loading States

```typescript
if (isLoading) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
      <CircularProgress />
    </Box>
  );
}
```

### Error States

```typescript
if (error) {
  return <Alert severity="error">Failed to load data</Alert>;
}
```

### Empty States

```typescript
if (!data || data.length === 0) {
  return (
    <Typography color="text.secondary" align="center">
      No data available
    </Typography>
  );
}
```

### Conditional Rendering with MUI Grid

```typescript
// MUI v7 uses size prop (not item prop)
<Grid container spacing={3}>
  {showChart && (
    <Grid size={{ xs: 12, lg: 6 }}>
      <ChartComponent />
    </Grid>
  )}
</Grid>
```

## Pull Request Checklist

Before submitting a PR:

- [ ] Code compiles without errors (`npm run build`)
- [ ] No ESLint warnings (`npm run lint`)
- [ ] Types are properly defined
- [ ] Components have proper loading/error states
- [ ] New files follow naming conventions
- [ ] Exports added to barrel files (`index.ts`)
- [ ] Documentation updated if needed

## Getting Help

- Check existing code for patterns
- Review the [Architecture Guide](./ARCHITECTURE.md)
- Ask in the team Slack channel
