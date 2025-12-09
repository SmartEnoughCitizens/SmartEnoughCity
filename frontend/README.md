# Hermes Frontend - Smart City Transport Analytics

A modern React frontend for the Hermes public transportation analytics system.

## Tech Stack

- **React 19** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router v7** - Client-side routing
- **MUI 7** - Material-UI components
- **React Query** - Server state management
- **Redux Toolkit** - Global UI state
- **Axios** - HTTP client
- **Recharts** - Data visualization
- **React Leaflet** - Map components

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend API running on http://localhost:8080

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

The app will be available at http://localhost:3000

## Project Structure

```
src/
├── api/              # API client functions
├── types/            # TypeScript type definitions
├── hooks/            # React Query hooks
├── store/            # Redux Toolkit slices
├── components/       # Reusable UI components
│   ├── auth/        # Authentication components
│   ├── charts/      # Chart components (Recharts)
│   ├── tables/      # Table components
│   └── map/         # Map components (Leaflet)
├── pages/            # Page components
├── layouts/          # Layout components
├── router/           # React Router configuration
├── theme/            # MUI theme configuration
├── utils/            # Utility functions
└── config/           # Configuration files
```

## Features

### Authentication
- JWT-based login with Keycloak
- Protected routes
- Auto-redirect on session expiry

### Dashboard
- Overview with key metrics
- Real-time bus trip updates
- Cycle station availability
- Interactive data visualizations

### Bus Dashboard
- Route selection
- Delay statistics and charts
- Trip update tables
- Real-time data

### Cycle Dashboard
- Station map view
- Availability filters
- Station statistics
- Interactive maps with Leaflet

### Notifications
- User-specific notifications
- Priority and type filtering
- Real-time updates

## API Integration

All backend endpoints are fully typed and integrated:

- `POST /api/auth/login` - Authentication
- `GET /api/v1/dashboard/bus` - Bus data
- `GET /api/v1/dashboard/cycle` - Cycle stations
- `GET /api/v1/notifications/{userId}` - User notifications
- And more...

## Development Guidelines

### State Management
- **Server state** → React Query (API data)
- **UI state** → Redux Toolkit (theme, sidebar, etc.)
- **Component state** → React useState

### Code Organization
- One component per file
- Co-locate related files
- Use TypeScript interfaces from `/types`
- API functions in `/api`
- React Query hooks in `/hooks`

### Styling
- Use MUI components and theme
- Responsive design with MUI Grid
- Dark/light theme support

## Environment Variables

Create a `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Build for Production

```bash
npm run build
```

Output will be in the `dist/` directory.

## Architecture Decisions

1. **React Query for API calls** - Automatic caching, refetching, and error handling
2. **Redux Toolkit for UI state** - Global state like theme, sidebar, auth status
3. **Lazy-loaded routes** - Code splitting for better performance
4. **MUI theme system** - Consistent design and dark mode support
5. **TypeScript throughout** - Type safety and better DX
6. **Modular structure** - Easy to maintain and extend

## License

MIT
