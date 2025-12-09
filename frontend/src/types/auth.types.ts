/**
 * Authentication types matching backend DTOs
 */

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  refreshToken: string;
  username: string;
  message: string;
}

export interface ErrorResponse {
  error: string;
}

export interface HealthResponse {
  status: string;
  message: string;
}
