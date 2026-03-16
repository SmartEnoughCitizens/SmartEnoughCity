/**
 * React Router configuration with lazy-loaded routes
 */

import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { CircularProgress, Box } from "@mui/material";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { DashboardLayout } from "@/layouts/DashboardLayout";
import { usePermissions } from "@/hooks/usePermissions";
import type { TransportCategory } from "@/config/permissions";

// ─── Category-level route guard ───────────────────────────────────────────────

/**
 * Wraps a route so that users without access to the given transport category
 * are redirected to the main dashboard instead of seeing the page.
 */
// eslint-disable-next-line react-refresh/only-export-components
const CategoryRoute = ({
  category,
  children,
}: {
  category: TransportCategory;
  children: React.ReactNode;
}) => {
  const { canViewCategory } = usePermissions();
  if (!canViewCategory(category)) {
    return <Navigate to="/dashboard" replace />;
  }
  return <>{children}</>;
};

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
const CarDashboard = lazy(() =>
  import("@/pages/CarDashboard").then((m) => ({ default: m.CarDashboard })),
);
const TrainDashboard = lazy(() =>
  import("@/pages/TrainDashboard").then((m) => ({ default: m.TrainDashboard })),
);
const NotificationsPage = lazy(() =>
  import("@/pages/NotificationsPage").then((m) => ({
    default: m.NotificationsPage,
  })),
);
const UserManagementPage = lazy(() =>
  import("@/pages/UserManagementPage").then((m) => ({
    default: m.UserManagementPage,
  })),
);
const ForgotPasswordPage = lazy(() =>
  import("@/pages/ForgotPasswordPage").then((m) => ({
    default: m.ForgotPasswordPage,
  })),
);
const ResetPasswordPage = lazy(() =>
  import("@/pages/ResetPasswordPage").then((m) => ({
    default: m.ResetPasswordPage,
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
    path: "/forgot-password",
    element: (
      <LazyWrapper>
        <ForgotPasswordPage />
      </LazyWrapper>
    ),
  },
  {
    path: "/reset-password",
    element: (
      <LazyWrapper>
        <ResetPasswordPage />
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
          <CategoryRoute category="bus">
            <LazyWrapper>
              <BusDashboard />
            </LazyWrapper>
          </CategoryRoute>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/cycle",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <CategoryRoute category="cycle">
            <LazyWrapper>
              <CycleDashboard />
            </LazyWrapper>
          </CategoryRoute>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/car",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <CategoryRoute category="car">
            <LazyWrapper>
              <CarDashboard />
            </LazyWrapper>
          </CategoryRoute>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/train",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <CategoryRoute category="train">
            <LazyWrapper>
              <TrainDashboard />
            </LazyWrapper>
          </CategoryRoute>
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
    path: "/dashboard/users",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <LazyWrapper>
            <UserManagementPage />
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
