import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  sidebarOpen: true,
  notifications: [],
  unreadCount: 0,
  isOffline: false,
  snackbar: {
    open: false,
    message: '',
    severity: 'info', // 'success', 'error', 'warning', 'info'
  },
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action) => {
      state.sidebarOpen = action.payload;
    },
    setNotifications: (state, action) => {
      state.notifications = action.payload;
      state.unreadCount = action.payload.filter(n => !n.read).length;
    },
    addNotification: (state, action) => {
      state.notifications.unshift(action.payload);
      if (!action.payload.read) {
        state.unreadCount += 1;
      }
    },
    markNotificationRead: (state, action) => {
      const notification = state.notifications.find(n => n.id === action.payload);
      if (notification && !notification.read) {
        notification.read = true;
        state.unreadCount -= 1;
      }
    },
    markAllNotificationsRead: (state) => {
      state.notifications.forEach(n => n.read = true);
      state.unreadCount = 0;
    },
    setOfflineMode: (state, action) => {
      state.isOffline = action.payload;
    },
    showSnackbar: (state, action) => {
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
export const selectSidebarOpen = (state) => state.ui.sidebarOpen;
export const selectNotifications = (state) => state.ui.notifications;
export const selectUnreadCount = (state) => state.ui.unreadCount;
export const selectIsOffline = (state) => state.ui.isOffline;
export const selectSnackbar = (state) => state.ui.snackbar;
