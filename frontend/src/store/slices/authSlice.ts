/**
 * Auth slice for Redux Toolkit
 * Manages authentication UI state (NOT server state - that's in React Query)
 */

import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { getRolesFromToken } from "@/utils/jwt";

interface AuthState {
  isAuthenticated: boolean;
  username: string | null;
  accessToken: string | null;
  roles: string[];
}

const storedToken = localStorage.getItem("accessToken");

const initialState: AuthState = {
  isAuthenticated: !!storedToken,
  username: localStorage.getItem("username"),
  accessToken: storedToken,
  roles: storedToken ? getRolesFromToken(storedToken) : [],
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setAuthenticated: (
      state,
      action: PayloadAction<{
        accessToken: string;
        username: string;
        roles: string[];
      }>,
    ) => {
      state.isAuthenticated = true;
      state.accessToken = action.payload.accessToken;
      state.username = action.payload.username;
      state.roles = action.payload.roles;
    },
    clearAuthentication: (state) => {
      state.isAuthenticated = false;
      state.accessToken = null;
      state.username = null;
      state.roles = [];
    },
  },
});

export const { setAuthenticated, clearAuthentication } = authSlice.actions;
export default authSlice.reducer;
