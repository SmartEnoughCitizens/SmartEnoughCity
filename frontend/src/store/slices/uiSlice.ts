/**
 * UI slice for Redux Toolkit
 * Manages global UI state (theme, sidebar, notifications, etc.)
 */

import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

interface UIState {
  sidebarOpen: boolean;
  theme: 'light' | 'dark';
  selectedRouteId: string | null;
  notificationBadgeCount: number;
}

const initialState: UIState = {
  sidebarOpen: true,
  theme: (localStorage.getItem('theme') as 'light' | 'dark') || 'light',
  selectedRouteId: null,
  notificationBadgeCount: 0,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
    },
    toggleTheme: (state) => {
      state.theme = state.theme === 'light' ? 'dark' : 'light';
      localStorage.setItem('theme', state.theme);
    },
    setTheme: (state, action: PayloadAction<'light' | 'dark'>) => {
      state.theme = action.payload;
      localStorage.setItem('theme', action.payload);
    },
    setSelectedRouteId: (state, action: PayloadAction<string | null>) => {
      state.selectedRouteId = action.payload;
    },
    setNotificationBadgeCount: (state, action: PayloadAction<number>) => {
      state.notificationBadgeCount = action.payload;
    },
  },
});

export const {
  toggleSidebar,
  setSidebarOpen,
  toggleTheme,
  setTheme,
  setSelectedRouteId,
  setNotificationBadgeCount,
} = uiSlice.actions;

export default uiSlice.reducer;
