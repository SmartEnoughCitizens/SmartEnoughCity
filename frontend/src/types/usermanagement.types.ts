/**
 * User management types matching backend DTOs
 */

export interface RegisterUserRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  password?: string;
}

export interface RegisterUserResponse {
  userId: string;
  username: string;
  email: string;
  role: string;
  message: string;
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
