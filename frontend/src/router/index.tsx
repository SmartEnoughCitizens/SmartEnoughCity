/**
 * React Router configuration with lazy-loaded routes
 */

import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { CircularProgress, Box } from "@mui/material";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { DashboardLayout } from "@/layouts/DashboardLayout";

// Lazy load pages
const LoginPage = lazy(() =>
  import("@/pages/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const Dashboard = lazy(() =>
  import("@/pages/Dashboard").then((m) => ({ default: m.Dashboard })),
);
const BusDashboard = lazy(() =>
  import("@/pages/BusDashboard").then((m) => ({ default: m.BusDashboard })),
);
const CycleDashboard = lazy(() =>
  import("@/pages/CycleDashboard").then((m) => ({ default: m.CycleDashboard })),
);
const NotificationsPage = lazy(() =>
  import("@/pages/NotificationsPage").then((m) => ({
    default: m.NotificationsPage,
  })),
);

// Loading fallback
// eslint-disable-next-line react-refresh/only-export-components
const LoadingFallback = () => (
  <Box
    sx={{
      display: "flex",
      justifyContent: "center",
      alignItems: "center",
      minHeight: "100vh",
    }}
  >
    <CircularProgress />
  </Box>
);

// Wrapper for lazy-loaded components
// eslint-disable-next-line react-refresh/only-export-components
const LazyWrapper = ({ children }: { children: React.ReactNode }) => (
  <Suspense fallback={<LoadingFallback />}>{children}</Suspense>
);

export const router = createBrowserRouter([
  {
    path: "/login",
    element: (
      <LazyWrapper>
        <LoginPage />
      </LazyWrapper>
    ),
  },
  {
    path: "/dashboard",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <LazyWrapper>
            <Dashboard />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/bus",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <LazyWrapper>
            <BusDashboard />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/cycle",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <LazyWrapper>
            <CycleDashboard />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/notifications",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <LazyWrapper>
            <NotificationsPage />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/",
    element: <Navigate to="/dashboard" replace />,
  },
  {
    path: "*",
    element: <Navigate to="/dashboard" replace />,
  },
]);
