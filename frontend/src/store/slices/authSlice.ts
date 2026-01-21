/**
 * Auth slice for Redux Toolkit
 * Manages authentication UI state (NOT server state - that's in React Query)
 */

import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

interface AuthState {
  isAuthenticated: boolean;
  username: string | null;
  accessToken: string | null;
}

const initialState: AuthState = {
  isAuthenticated: !!localStorage.getItem("accessToken"),
  username: localStorage.getItem("username"),
  accessToken: localStorage.getItem("accessToken"),
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
      }>,
    ) => {
      state.isAuthenticated = true;
      state.accessToken = action.payload.accessToken;
      state.username = action.payload.username;
    },
    clearAuthentication: (state) => {
      state.isAuthenticated = false;
      state.accessToken = null;
      state.username = null;
    },
  },
});

export const { setAuthenticated, clearAuthentication } = authSlice.actions;
export default authSlice.reducer;
