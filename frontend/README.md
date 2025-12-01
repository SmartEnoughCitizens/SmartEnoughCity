# Sustainable City Management System - Frontend

A production-ready ReactJS frontend for the Sustainable City Management System, built to integrate seamlessly with a Java Spring Boot backend.

## Tech Stack

- **React 18** - Modern React with hooks
- **Vite** - Fast build tool and dev server
- **React Router v6** - Client-side routing
- **Redux Toolkit** - State management
- **Material UI (MUI)** - UI component library
- **Axios** - HTTP client with interceptors
- **Recharts** - Data visualization
- **Leaflet** - Interactive maps

## Features

### Authentication
- JWT-based authentication (Spring Boot compatible)
- Token stored in Redux state
- Automatic token injection via Axios interceptors
- Protected routes with role-based access control

### User Roles
- Government Admin
- City Manager
- Service Provider Admin
- Service Provider User

### Pages
1. **Login & Registration** - JWT authentication
2. **Dashboard** - Overview with charts and metrics
3. **Transport Indicators** - Car, Cycle, Bus, Train, Tram, Pedestrian
4. **Events & Construction** - Manage city events and construction projects
5. **Simulation Center** - Run traffic simulations
6. **Notification Center** - System notifications and alerts
7. **User Management** - Manage users (Admin only)
8. **System Status** - System health monitoring (Gov Admin only)

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── common/          # Reusable components
│   │   │   ├── CityMap.jsx
│   │   │   ├── ChartCard.jsx
│   │   │   ├── MetricCard.jsx
│   │   │   ├── RecommendationCard.jsx
│   │   │   ├── NotificationList.jsx
│   │   │   ├── LoadingSpinner.jsx
│   │   │   └── ProtectedRoute.jsx
│   │   ├── layout/          # Layout components
│   │   │   ├── AppLayout.jsx
│   │   │   ├── Navbar.jsx
│   │   │   └── Sidebar.jsx
│   │   └── indicators/      # Indicator components
│   │       └── TransportIndicator.jsx
│   ├── pages/
│   │   ├── auth/            # Authentication pages
│   │   ├── dashboard/       # Dashboard page
│   │   ├── indicators/      # Indicator pages
│   │   ├── simulation/      # Simulation center
│   │   ├── notifications/   # Notification center
│   │   ├── users/           # User management
│   │   └── system/          # System status
│   ├── redux/
│   │   ├── slices/
│   │   │   ├── authSlice.js
│   │   │   └── uiSlice.js
│   │   └── store.js
│   ├── services/            # API service layer
│   │   ├── api.js           # Axios instance with interceptors
│   │   ├── authService.js
│   │   ├── userService.js
│   │   ├── indicatorService.js
│   │   ├── simulationService.js
│   │   ├── notificationService.js
│   │   ├── dashboardService.js
│   │   └── systemService.js
│   ├── utils/
│   │   ├── theme.js         # MUI theme configuration
│   │   └── helpers.js       # Utility functions
│   ├── constants/
│   │   └── index.js         # API endpoints, roles, constants
│   ├── App.jsx              # Main app with routes
│   └── main.jsx             # Entry point
├── package.json
├── vite.config.js
└── README.md
```

## Getting Started

### Prerequisites
- Node.js (v16 or higher)
- npm or yarn
- Backend API running on `http://localhost:8080`

### Installation

1. Clone the repository and navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create a `.env` file (optional, defaults are set):
```bash
cp .env.example .env
```

4. Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Build for Production

```bash
npm run build
```

The production build will be in the `dist/` directory.

### Preview Production Build

```bash
npm run preview
```

## API Integration

### Backend Endpoints
The frontend expects the following Spring Boot REST API structure:

#### Authentication
- `POST /api/auth/login` - Login with credentials
- `POST /api/auth/register/city-manager` - Register city manager
- `POST /api/auth/register/service-provider-admin` - Register service provider admin

#### Users
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users/service-provider` - Create service provider user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

#### Indicators
- `GET /api/indicators/car` - Car indicators
- `GET /api/indicators/cycle` - Cycle indicators
- `GET /api/indicators/bus` - Bus indicators
- `GET /api/indicators/train` - Train indicators
- `GET /api/indicators/tram` - Tram indicators
- `GET /api/indicators/pedestrian` - Pedestrian indicators
- `GET /api/indicators/events` - Events indicators
- `GET /api/indicators/construction` - Construction indicators

#### Dashboard
- `GET /api/dashboard/stats` - Dashboard statistics
- `GET /api/dashboard/overview` - Dashboard overview

#### Simulations
- `GET /api/simulations` - Get all simulations
- `GET /api/simulations/{id}` - Get simulation by ID
- `POST /api/simulations/run` - Run new simulation
- `DELETE /api/simulations/{id}` - Delete simulation

#### Notifications
- `GET /api/notifications` - Get all notifications
- `PUT /api/notifications/{id}/read` - Mark notification as read
- `PUT /api/notifications/mark-all-read` - Mark all as read

#### System
- `GET /api/system/status` - System status
- `GET /api/system/health` - System health

### Authentication Flow

1. User logs in via `/api/auth/login`
2. Backend returns JWT token in response body:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "role": "ROLE_GOVERNMENT_ADMIN",
    ...
  }
}
```
3. Frontend stores token in Redux and localStorage
4. Axios interceptor adds `Authorization: Bearer <token>` to all requests
5. On 401/403 response, user is redirected to login

## Role-Based Access Control

Routes are protected based on user roles:

- **Public**: Login, Register
- **All Authenticated**: Dashboard, Indicators, Simulation, Notifications
- **Admin + Service Provider Admin**: User Management
- **Government Admin Only**: System Status

## Development

### Code Structure
- Use functional components with hooks
- Redux Toolkit for state management
- Material UI components for consistency
- Axios services for API calls
- Protected routes for authorization

### Adding a New Page

1. Create component in `src/pages/your-section/YourPage.jsx`
2. Add route in `src/App.jsx`
3. Add menu item in `src/components/layout/Sidebar.jsx`
4. Create service if needed in `src/services/yourService.js`

### Styling
- Use MUI's `sx` prop for inline styles
- Theme configuration in `src/utils/theme.js`
- Responsive design with MUI Grid

## Environment Variables

- `VITE_API_BASE_URL` - Backend API base URL (default: via proxy)

## Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## License

Apache
