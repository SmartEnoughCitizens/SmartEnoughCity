/**
 * React Router configuration with lazy-loaded routes
 */

import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { CircularProgress, Box } from "@mui/material";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { DashboardLayout } from "@/layouts/DashboardLayout";
import { TRANSPORT_ACCESS, getLandingPage } from "@/types";
import { useAppSelector } from "@/store/hooks";

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
const MiscDashboard = lazy(() =>
  import("@/pages/MiscDashboard").then((m) => ({ default: m.MiscDashboard })),
);
const TramDashboard = lazy(() =>
  import("@/pages/TramDashboard").then((m) => ({ default: m.TramDashboard })),
);

// eslint-disable-next-line react-refresh/only-export-components
const SmartRedirect = () => {
  const { roles } = useAppSelector((state) => state.auth);
  return <Navigate to={getLandingPage(roles)} replace />;
};
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
      <ProtectedRoute allowedRoles={["City_Manager"]}>
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
      <ProtectedRoute allowedRoles={TRANSPORT_ACCESS.bus}>
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
      <ProtectedRoute allowedRoles={TRANSPORT_ACCESS.cycle}>
        <DashboardLayout>
          <LazyWrapper>
            <CycleDashboard />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/car",
    element: (
      <ProtectedRoute allowedRoles={TRANSPORT_ACCESS.car}>
        <DashboardLayout>
          <LazyWrapper>
            <CarDashboard />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/train",
    element: (
      <ProtectedRoute allowedRoles={TRANSPORT_ACCESS.train}>
        <DashboardLayout>
          <LazyWrapper>
            <TrainDashboard />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/tram",
    element: (
      <ProtectedRoute allowedRoles={TRANSPORT_ACCESS.tram}>
        <DashboardLayout>
          <LazyWrapper>
            <TramDashboard />
          </LazyWrapper>
        </DashboardLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: "/dashboard/misc",
    element: (
      <ProtectedRoute>
        <DashboardLayout>
          <LazyWrapper>
            <MiscDashboard />
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
    element: <SmartRedirect />,
  },
  {
    path: "*",
    element: <SmartRedirect />,
  },
]);
