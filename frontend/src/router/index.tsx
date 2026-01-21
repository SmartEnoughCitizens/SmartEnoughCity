/**
 * React Router configuration with lazy-loaded routes
 */

import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { DashboardLayout } from '@/layouts/DashboardLayout';

// Lazy load pages
const LoginPage = lazy(() =>
  import('@/pages/LoginPage').then((m) => ({ default: m.LoginPage }))
);
const Dashboard = lazy(() =>
  import('@/pages/Dashboard').then((m) => ({ default: m.Dashboard }))
);
const BusDashboard = lazy(() =>
  import('@/pages/BusDashboard').then((m) => ({ default: m.BusDashboard }))
);
const CycleDashboard = lazy(() =>
  import('@/pages/CycleDashboard').then((m) => ({ default: m.CycleDashboard }))
);
const NotificationsPage = lazy(() =>
  import('@/pages/NotificationsPage').then((m) => ({ default: m.NotificationsPage }))
);

// Loading fallback
const LoadingFallback = () => (
  <Box
    sx={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
    }}
  >
    <CircularProgress />
  </Box>
);

// Protected dashboard layout wrapper
const ProtectedDashboardLayout = () => (
  <ProtectedRoute>
    <DashboardLayout>
      <Suspense fallback={<LoadingFallback />}>
        <Outlet />
      </Suspense>
    </DashboardLayout>
  </ProtectedRoute>
);

export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <Suspense fallback={<LoadingFallback />}>
        <LoginPage />
      </Suspense>
    ),
  },
  {
    path: '/dashboard',
    element: <ProtectedDashboardLayout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'bus', element: <BusDashboard /> },
      { path: 'cycle', element: <CycleDashboard /> },
      { path: 'notifications', element: <NotificationsPage /> },
    ],
  },
  {
    path: '/',
    element: <Navigate to="/dashboard" replace />,
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />,
  },
]);
