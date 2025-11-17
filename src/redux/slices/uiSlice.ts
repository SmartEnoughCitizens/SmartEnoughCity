import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Notification } from '../../types';

interface SnackbarState {
  open: boolean;
  message: string;
  severity: 'success' | 'error' | 'warning' | 'info';
}

interface UIState {
  sidebarOpen: boolean;
  notifications: Notification[];
  unreadCount: number;
  isOffline: boolean;
  snackbar: SnackbarState;
}

const initialState: UIState = {
  sidebarOpen: true,
  notifications: [],
  unreadCount: 0,
  isOffline: false,
  snackbar: {
    open: false,
    message: '',
    severity: 'info',
  },
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
    setNotifications: (state, action: PayloadAction<Notification[]>) => {
      state.notifications = action.payload;
      state.unreadCount = action.payload.filter((n) => !n.read).length;
    },
    addNotification: (state, action: PayloadAction<Notification>) => {
      state.notifications.unshift(action.payload);
      if (!action.payload.read) {
        state.unreadCount += 1;
      }
    },
    markNotificationRead: (state, action: PayloadAction<string>) => {
      const notification = state.notifications.find((n) => n.id === action.payload);
      if (notification && !notification.read) {
        notification.read = true;
        state.unreadCount -= 1;
      }
    },
    markAllNotificationsRead: (state) => {
      state.notifications.forEach((n) => (n.read = true));
      state.unreadCount = 0;
    },
    setOfflineMode: (state, action: PayloadAction<boolean>) => {
      state.isOffline = action.payload;
    },
    showSnackbar: (
      state,
      action: PayloadAction<{ message: string; severity?: 'success' | 'error' | 'warning' | 'info' }>
    ) => {
      state.snackbar = {
        open: true,
        message: action.payload.message,
        severity: action.payload.severity || 'info',
      };
    },
    hideSnackbar: (state) => {
      state.snackbar.open = false;
    },
  },
});

export const {
  toggleSidebar,
  setSidebarOpen,
  setNotifications,
  addNotification,
  markNotificationRead,
  markAllNotificationsRead,
  setOfflineMode,
  showSnackbar,
  hideSnackbar,
} = uiSlice.actions;

export default uiSlice.reducer;

// Selectors
export const selectSidebarOpen = (state: { ui: UIState }) => state.ui.sidebarOpen;
export const selectNotifications = (state: { ui: UIState }) => state.ui.notifications;
export const selectUnreadCount = (state: { ui: UIState }) => state.ui.unreadCount;
export const selectIsOffline = (state: { ui: UIState }) => state.ui.isOffline;
export const selectSnackbar = (state: { ui: UIState }) => state.ui.snackbar;
