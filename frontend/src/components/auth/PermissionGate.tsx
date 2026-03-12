/**
 * PermissionGate — Declarative RBAC guard component
 *
 * Renders children only when the current user satisfies the required
 * permission. Optionally renders a `fallback` when access is denied
 * (useful for showing disabled buttons or tooltip hints).
 *
 * ── Usage ──────────────────────────────────────────────────────────────────
 *
 * // Write-gate: only Admin-level roles can dismiss notifications
 * <PermissionGate require="write">
 *   <IconButton onClick={handleDismiss}><DeleteIcon /></IconButton>
 * </PermissionGate>
 *
 * // With fallback (disabled button for read-only users)
 * <PermissionGate
 *   require="write"
 *   fallback={
 *     <Tooltip title="Read-only access">
 *       <span><IconButton disabled><DeleteIcon /></IconButton></span>
 *     </Tooltip>
 *   }
 * >
 *   <IconButton onClick={handleDismiss}><DeleteIcon /></IconButton>
 * </PermissionGate>
 *
 * // Category-gate: only users with bus access see bus controls
 * <PermissionGate require="category" category="bus">
 *   <BusOnlyFeature />
 * </PermissionGate>
 *
 * ───────────────────────────────────────────────────────────────────────────
 */

import { usePermissions } from "@/hooks/usePermissions";
import type { TransportCategory } from "@/config/permissions";

// ─── Prop Types ───────────────────────────────────────────────────────────────

interface WriteGateProps {
  require: "write";
  category?: never;
  fallback?: React.ReactNode;
  children: React.ReactNode;
}

interface CategoryGateProps {
  require: "category";
  category: TransportCategory;
  fallback?: React.ReactNode;
  children: React.ReactNode;
}

type PermissionGateProps = WriteGateProps | CategoryGateProps;

// ─── Component ────────────────────────────────────────────────────────────────

export const PermissionGate = ({
  require,
  category,
  fallback = null,
  children,
}: PermissionGateProps) => {
  const { canWrite, canViewCategory } = usePermissions();

  const hasPermission =
    require === "write"
      ? canWrite
      : // category is always defined when require === "category" (enforced by discriminated union)
        category !== undefined && canViewCategory(category);

  return hasPermission ? <>{children}</> : <>{fallback}</>;
};
