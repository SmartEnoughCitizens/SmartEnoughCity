/**
 * Protected route wrapper component
 */

import { Navigate } from "react-router-dom";
import { useAppSelector } from "@/store/hooks";
import { getLandingPage } from "@/types";

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: string[];
}

export const ProtectedRoute = ({
  children,
  allowedRoles,
}: ProtectedRouteProps) => {
  const { isAuthenticated, roles } = useAppSelector((state) => state.auth);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.some((r) => roles.includes(r))) {
    return <Navigate to={getLandingPage(roles)} replace />;
  }

  return <>{children}</>;
};
