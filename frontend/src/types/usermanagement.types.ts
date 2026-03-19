/**
 * User management types matching backend DTOs
 */

export interface RegisterUserRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface RegisterUserResponse {
  userId: string;
  username: string;
  email: string;
  role: string;
  message: string;
}

export interface UserInfo {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface UserProfile {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export const ALLOWED_ROLES = [
  "City_Manager",
  "Bus_Admin",
  "Bus_Provider",
  "Cycle_Admin",
  "Cycle_Provider",
  "Train_Admin",
  "Train_Provider",
  "Tram_Admin",
  "Tram_Provider",
] as const;

/**
 * Maps each creator role to the roles it can create.
 * Mirrors backend CREATE_PERMISSIONS in UserManagementController.
 */
export const CREATE_PERMISSIONS: Record<string, string[]> = {
  Government_Admin: ["City_Manager"],
  City_Manager: ["Bus_Admin", "Cycle_Admin", "Train_Admin", "Tram_Admin"],
  Bus_Admin: ["Bus_Provider"],
  Cycle_Admin: ["Cycle_Provider"],
  Train_Admin: ["Train_Provider"],
  Tram_Admin: ["Tram_Provider"],
};

/**
 * Maps each transport mode to the roles that can access its data.
 */
export const TRANSPORT_ACCESS: Record<string, string[]> = {
  bus:   ["City_Manager", "Bus_Admin",   "Bus_Provider"],
  train: ["City_Manager", "Train_Admin", "Train_Provider"],
  cycle: ["City_Manager", "Cycle_Admin", "Cycle_Provider"],
  tram:  ["City_Manager", "Tram_Admin",  "Tram_Provider"],
  car:   ["City_Manager"],
};

/**
 * Returns true if the user has access to the given transport mode.
 */
export function canAccessTransport(userRoles: string[], transport: string): boolean {
  return (TRANSPORT_ACCESS[transport] ?? []).some((r) => userRoles.includes(r));
}

/**
 * Returns the appropriate landing page path based on the user's roles.
 */
export function getLandingPage(userRoles: string[]): string {
  if (userRoles.includes("City_Manager"))                                              return "/dashboard";
  if (userRoles.some((r) => ["Bus_Admin",   "Bus_Provider"  ].includes(r)))           return "/dashboard/bus";
  if (userRoles.some((r) => ["Train_Admin", "Train_Provider"].includes(r)))           return "/dashboard/train";
  if (userRoles.some((r) => ["Cycle_Admin", "Cycle_Provider"].includes(r)))           return "/dashboard/cycle";
  if (userRoles.some((r) => ["Tram_Admin",  "Tram_Provider" ].includes(r)))           return "/dashboard/tram";
  return "/dashboard/notifications";
}

/**
 * Role priority from highest to lowest. Keycloak composite roles may add
 * sub-roles to the JWT (e.g. City_Manager token also contains Bus_Admin),
 * so we only use the highest-level role for determining creatable roles.
 */
const ROLE_PRIORITY = [
  "Government_Admin",
  "City_Manager",
  "Bus_Admin",
  "Cycle_Admin",
  "Train_Admin",
  "Tram_Admin",
];

/**
 * Returns the roles a user can create based on their highest-level role.
 */
export function getCreatableRoles(userRoles: string[]): string[] {
  const primaryRole = ROLE_PRIORITY.find((r) => userRoles.includes(r));
  return primaryRole ? (CREATE_PERMISSIONS[primaryRole] ?? []) : [];
}
