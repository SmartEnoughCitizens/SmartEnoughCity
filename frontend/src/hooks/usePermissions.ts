/**
 * usePermissions — RBAC permission hook
 *
 * Derives the current user's access rights from their Keycloak roles
 * stored in Redux auth state. Components consume this hook instead of
 * inspecting raw role arrays directly.
 */

import { useMemo } from "react";
import { useAppSelector } from "@/store/hooks";
import {
  getAllowedCategories,
  hasWritePermission,
  canAccessCategory as _canAccessCategory,
  GLOBAL_ADMIN_ROLES,
  type TransportCategory,
} from "@/config/permissions";

export interface UsePermissionsReturn {
  /** Raw roles array from JWT */
  roles: string[];

  /** True when the user is a Government_Admin or City_Manager */
  isGlobalAdmin: boolean;

  /**
   * Transport categories this user can access.
   * Returns `"all"` for global admins; a typed array for scoped roles.
   */
  allowedCategories: TransportCategory[] | "all";

  /**
   * True when the user holds at least one write-capable (Admin-level) role.
   * Provider roles are always read-only.
   *
   * Governs:
   *  - Dismiss / delete notifications
   *  - Act on (apply / reject) recommendations
   *  - Export / download data
   */
  canWrite: boolean;

  /** Check whether a specific transport category is accessible */
  canViewCategory: (category: TransportCategory) => boolean;
}

export const usePermissions = (): UsePermissionsReturn => {
  const { roles } = useAppSelector((state) => state.auth);

  return useMemo(() => {
    const allowedCategories = getAllowedCategories(roles);
    const isGlobalAdmin = roles.some((r) => GLOBAL_ADMIN_ROLES.includes(r));
    const canWrite = hasWritePermission(roles);

    return {
      roles,
      isGlobalAdmin,
      allowedCategories,
      canWrite,
      canViewCategory: (category: TransportCategory) =>
        _canAccessCategory(roles, category),
    };
  }, [roles]);
};
