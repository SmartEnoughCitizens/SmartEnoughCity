# Hermes Frontend

A React-based dashboard for Smart City Transport Analytics, providing real-time visualization of bus and cycle station data in Dublin.

## Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

The app runs at `http://localhost:5173` in development.

## Tech Stack

| Category | Technology |
|----------|------------|
| Framework | React 19 with TypeScript |
| Build Tool | Vite 7 |
| UI Library | Material UI (MUI) 7 |
| State Management | Redux Toolkit + React Query |
| Routing | React Router 7 |
| Charts | Recharts |
| Maps | React Leaflet |
| HTTP Client | Axios |

## Project Structure

```
src/
├── api/              # API client modules
├── components/       # Reusable UI components
│   ├── auth/         # Authentication components
│   ├── charts/       # Data visualization
│   ├── map/          # Leaflet map components
│   └── tables/       # Data tables
├── config/           # App configuration
├── hooks/            # Custom React hooks
├── layouts/          # Page layouts
├── pages/            # Route pages
├── router/           # Route definitions
├── store/            # Redux store & slices
├── theme/            # MUI theme config
├── types/            # TypeScript interfaces
└── utils/            # Utility functions
```

## Documentation

- [Architecture Guide](./docs/ARCHITECTURE.md) - System design and data flow
- [Contributing Guide](./docs/CONTRIBUTING.md) - How to contribute
- [API Reference](./docs/API.md) - Backend API integration

## Environment

The app expects these backend services:

| Service | Development URL |
|---------|-----------------|
| Main API | `http://localhost:8080` |
| Auth | `/api/auth/*` |
| Dashboard | `/api/v1/dashboard/*` |
| Notifications | `/notification/v1/*` |

## Scripts

```bash
npm run dev      # Start dev server with HMR
npm run build    # Type-check and build for production
npm run preview  # Preview production build
npm run lint     # Run ESLint
```

## License

Proprietary - Hermes Transport Analytics
