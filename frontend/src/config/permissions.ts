/**
 * RBAC Permission Configuration
 *
 * Single source of truth for what each role can see (transport categories)
 * and what actions they can perform (write vs. read-only).
 *
 * Roles are sourced from Keycloak JWT realm_access.roles.
 */

// ─── Types ────────────────────────────────────────────────────────────────────

/** Transport category identifiers — match the route path segments */
export type TransportCategory = "bus" | "cycle" | "car" | "train" | "tram";

// ─── Role → Category mapping ──────────────────────────────────────────────────

/**
 * Roles that have unrestricted access to ALL categories.
 * Any user bearing one of these roles sees the full sidebar.
 */
export const GLOBAL_ADMIN_ROLES: readonly string[] = [
  "Government_Admin",
  "City_Manager",
];

/**
 * Explicit category allowlist for transport-scoped roles.
 * Roles not listed here are assumed to have full access (safe fallback).
 */
export const ROLE_CATEGORY_ACCESS: Readonly<
  Record<string, TransportCategory[]>
> = {
  Bus_Admin: ["bus"],
  Bus_Provider: ["bus"],
  Cycle_Admin: ["cycle"],
  Cycle_Provider: ["cycle"],
  Train_Admin: ["train"],
  Train_Provider: ["train"],
  Tram_Admin: ["tram"],
  Tram_Provider: ["tram"],
};

// ─── Write permissions ────────────────────────────────────────────────────────

/**
 * Roles that are allowed to perform mutating/write actions.
 * Provider roles are intentionally excluded → read-only.
 *
 * Write actions include:
 *  - Dismiss / delete notifications
 *  - Act on (apply / reject) recommendations
 *  - Export / download data
 */
export const WRITE_ROLES: readonly string[] = [
  "Government_Admin",
  "City_Manager",
  "Bus_Admin",
  "Cycle_Admin",
  "Train_Admin",
  "Tram_Admin",
];

// ─── Helper functions ─────────────────────────────────────────────────────────

/**
 * Returns the transport categories accessible to a user based on their roles.
 * Returns `"all"` for global admins and as a safe fallback for unknown roles.
 */
export function getAllowedCategories(
  userRoles: string[],
): TransportCategory[] | "all" {
  if (userRoles.some((r) => GLOBAL_ADMIN_ROLES.includes(r))) {
    return "all";
  }

  const categories = new Set<TransportCategory>();
  for (const role of userRoles) {
    const allowed = ROLE_CATEGORY_ACCESS[role];
    if (allowed) {
      for (const c of allowed) {
        categories.add(c);
      }
    }
  }

  // Unknown roles → full access (safe fallback; backend enforces hard limits)
  return categories.size > 0 ? [...categories] : "all";
}

/**
 * Returns `true` if the user holds at least one write-capable role.
 */
export function hasWritePermission(userRoles: string[]): boolean {
  return userRoles.some((r) => WRITE_ROLES.includes(r));
}

/**
 * Returns `true` if the user can access a specific transport category.
 */
export function canAccessCategory(
  userRoles: string[],
  category: TransportCategory,
): boolean {
  const allowed = getAllowedCategories(userRoles);
  if (allowed === "all") return true;
  return allowed.includes(category);
}

/**
 * Human-readable label for each transport category.
 */
export const CATEGORY_LABELS: Record<TransportCategory, string> = {
  bus: "Bus",
  cycle: "Cycle",
  car: "Car",
  train: "Train",
  tram: "Tram",
};

/**
 * Returns a short display label describing a role's access scope.
 * Used in the User Management table.
 */
export function getRoleAccessLabel(role: string): string {
  if (GLOBAL_ADMIN_ROLES.includes(role)) return "All categories";
  const cats = ROLE_CATEGORY_ACCESS[role];
  if (!cats || cats.length === 0) return "All categories";
  return cats.map((c) => CATEGORY_LABELS[c]).join(", ");
}

/**
 * Returns whether a role is read-only (Provider) or has write access (Admin).
 */
export function getRoleAccessLevel(role: string): "read-write" | "read-only" {
  return WRITE_ROLES.includes(role) ? "read-write" : "read-only";
}
