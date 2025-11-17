import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';
import theme from './utils/theme';

// Layout
import AppLayout from './components/layout/AppLayout';

// Auth Pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

// Dashboard
import Dashboard from './pages/dashboard/Dashboard';

// Indicators
import CarIndicator from './pages/indicators/CarIndicator';
import CycleIndicator from './pages/indicators/CycleIndicator';
import BusIndicator from './pages/indicators/BusIndicator';
import TrainIndicator from './pages/indicators/TrainIndicator';
import TramIndicator from './pages/indicators/TramIndicator';
import PedestrianIndicator from './pages/indicators/PedestrianIndicator';
import EventsIndicator from './pages/indicators/EventsIndicator';
import ConstructionIndicator from './pages/indicators/ConstructionIndicator';

// Other Pages
import SimulationCenter from './pages/simulation/SimulationCenter';
import NotificationCenter from './pages/notifications/NotificationCenter';
import UserManagement from './pages/users/UserManagement';
import SystemStatus from './pages/system/SystemStatus';

// Components
import ProtectedRoute from './components/common/ProtectedRoute';

// Constants
import { ROLES } from './constants';

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            {/* Redirect root to dashboard */}
            <Route index element={<Navigate to="/dashboard" replace />} />

            {/* Dashboard */}
            <Route path="dashboard" element={<Dashboard />} />

            {/* Transport Indicators */}
            <Route path="indicators/car" element={<CarIndicator />} />
            <Route path="indicators/cycle" element={<CycleIndicator />} />
            <Route path="indicators/bus" element={<BusIndicator />} />
            <Route path="indicators/train" element={<TrainIndicator />} />
            <Route path="indicators/tram" element={<TramIndicator />} />
            <Route path="indicators/pedestrian" element={<PedestrianIndicator />} />
            <Route path="indicators/events" element={<EventsIndicator />} />
            <Route path="indicators/construction" element={<ConstructionIndicator />} />

            {/* Simulation */}
            <Route path="simulation" element={<SimulationCenter />} />

            {/* Notifications */}
            <Route path="notifications" element={<NotificationCenter />} />

            {/* User Management - Admin and Service Provider Admin only */}
            <Route
              path="users"
              element={
                <ProtectedRoute allowedRoles={[ROLES.GOVERNMENT_ADMIN, ROLES.SERVICE_PROVIDER_ADMIN]}>
                  <UserManagement />
                </ProtectedRoute>
              }
            />

            {/* System Status - Government Admin only */}
            <Route
              path="system-status"
              element={
                <ProtectedRoute allowedRoles={[ROLES.GOVERNMENT_ADMIN]}>
                  <SystemStatus />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Catch all - redirect to dashboard */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;
