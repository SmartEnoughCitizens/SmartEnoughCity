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
