/**
 * React Router configuration with lazy-loaded routes
 * Simplified to single /dashboard route - DashboardLayout handles view switching internally
 */

import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { CircularProgress, Box } from "@mui/material";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { DashboardLayout } from "@/layouts/DashboardLayout";
import { getLandingPage } from "@/types";
import { useAppSelector } from "@/store/hooks";

// Lazy load pages
const LoginPage = lazy(() =>
  import("@/pages/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const PublicDisruptionPage = lazy(() =>
  import("@/pages/PublicDisruptionPage").then((m) => ({
    default: m.PublicDisruptionPage,
  })),
);
const PublicEventPage = lazy(() =>
  import("@/pages/PublicEventPage").then((m) => ({
    default: m.PublicEventPage,
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
    path: "/public/disruption/:id",
    element: (
      <LazyWrapper>
        <PublicDisruptionPage />
      </LazyWrapper>
    ),
  },
  {
    path: "/public/event/:id",
    element: (
      <LazyWrapper>
        <PublicEventPage />
      </LazyWrapper>
    ),
  },
  {
    path: "/dashboard/*",
    element: (
      <ProtectedRoute>
        <DashboardLayout />
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
