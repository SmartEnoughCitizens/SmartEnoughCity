/**
 * Main App component with all providers
 */

import { useEffect } from "react";
import { RouterProvider } from "react-router-dom";
import { Provider as ReduxProvider } from "react-redux";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { ThemeProvider, CssBaseline } from "@mui/material";
import { store } from "@/store";
import { useAppSelector } from "@/store/hooks";
import { getTheme } from "@/theme";
import { router } from "@/router";

console.log("[App] Starting application initialization...");

// 30 days in milliseconds
const THIRTY_DAYS = 1000 * 60 * 60 * 24 * 30;

// Create QueryClient with VERY long cache times for persistent-like behavior
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnMount: false,
      refetchOnReconnect: false,
      staleTime: THIRTY_DAYS, // Data is fresh for 30 days
      gcTime: THIRTY_DAYS, // Keep in memory cache for 30 days
    },
  },
});

console.log("[App] QueryClient initialized with 30-day cache");

// Theme wrapper to access Redux state
const ThemedApp = () => {
  console.log("[ThemedApp] Rendering...");
  const theme = useAppSelector((state) => state.ui.theme);
  console.log("[ThemedApp] Theme:", theme);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  return (
    <ThemeProvider theme={getTheme(theme)}>
      <CssBaseline />
      <RouterProvider router={router} />
      <ReactQueryDevtools initialIsOpen={false} />
    </ThemeProvider>
  );
};

// Main App component
function App() {
  console.log("[App] Rendering main App component");

  return (
    <ReduxProvider store={store}>
      <QueryClientProvider client={queryClient}>
        <ThemedApp />
      </QueryClientProvider>
    </ReduxProvider>
  );
}

console.log("[App] App component defined, ready to export");

export default App;
