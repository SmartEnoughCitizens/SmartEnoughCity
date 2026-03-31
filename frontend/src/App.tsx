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

// 30 days in milliseconds
const THIRTY_DAYS = 1000 * 60 * 60 * 24 * 30;

// Create QueryClient with long cache times — all dashboards are always mounted,
// so data fetched on login stays fresh for the entire session.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnMount: false,
      refetchOnReconnect: false,
      staleTime: THIRTY_DAYS,
      gcTime: THIRTY_DAYS,
    },
  },
});

// Theme wrapper to access Redux state
const ThemedApp = () => {
  const theme = useAppSelector((state) => state.ui.theme);

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
  return (
    <ReduxProvider store={store}>
      <QueryClientProvider client={queryClient}>
        <ThemedApp />
      </QueryClientProvider>
    </ReduxProvider>
  );
}

export default App;
