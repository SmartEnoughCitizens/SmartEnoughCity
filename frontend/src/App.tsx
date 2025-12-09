/**
 * Main App component with all providers
 */

import { RouterProvider } from 'react-router-dom';
import { Provider as ReduxProvider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { store } from '@/store';
import { useAppSelector } from '@/store/hooks';
import { getTheme } from '@/theme';
import { router } from '@/router';

// Create QueryClient
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30000,
    },
  },
});

// Theme wrapper to access Redux state
const ThemedApp = () => {
  const theme = useAppSelector((state) => state.ui.theme);

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
