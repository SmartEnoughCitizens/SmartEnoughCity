/**
 * MUI theme configuration - Map-centric design
 * Glass-panel aesthetic with semi-transparent overlays
 */

import { createTheme, type ThemeOptions } from "@mui/material/styles";

const commonTypography = {
  fontFamily: [
    "Inter",
    "-apple-system",
    "BlinkMacSystemFont",
    '"Segoe UI"',
    "Roboto",
    '"Helvetica Neue"',
    "Arial",
    "sans-serif",
  ].join(","),
  h1: { fontSize: "2rem", fontWeight: 700, letterSpacing: "-0.02em" },
  h2: { fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.01em" },
  h3: { fontSize: "1.25rem", fontWeight: 600 },
  h4: { fontSize: "1.125rem", fontWeight: 600 },
  h5: { fontSize: "1rem", fontWeight: 600 },
  h6: {
    fontSize: "0.875rem",
    fontWeight: 600,
    textTransform: "uppercase" as const,
    letterSpacing: "0.05em",
  },
  body2: { fontSize: "0.8125rem" },
  caption: { fontSize: "0.75rem", letterSpacing: "0.02em" },
};

const lightThemeOptions: ThemeOptions = {
  palette: {
    mode: "light",
    primary: {
      main: "#2563eb",
      light: "#60a5fa",
      dark: "#1d4ed8",
      contrastText: "#fff",
    },
    secondary: {
      main: "#7c3aed",
      light: "#a78bfa",
      dark: "#5b21b6",
      contrastText: "#fff",
    },
    success: { main: "#16a34a", light: "#4ade80", dark: "#15803d" },
    warning: { main: "#ea580c", light: "#fb923c", dark: "#c2410c" },
    error: { main: "#dc2626", light: "#f87171", dark: "#b91c1c" },
    info: { main: "#0891b2", light: "#22d3ee", dark: "#0e7490" },
    background: {
      default: "#f1f5f9",
      paper: "rgba(255, 255, 255, 0.92)",
    },
    divider: "rgba(0, 0, 0, 0.06)",
  },
  typography: commonTypography,
  shape: { borderRadius: 12 },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: "none", fontWeight: 600, borderRadius: 8 },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
          backdropFilter: "blur(12px)",
          border: "1px solid rgba(0, 0, 0, 0.06)",
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 500, fontSize: "0.75rem" },
        sizeSmall: { height: 22 },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: "1px solid rgba(0, 0, 0, 0.04)",
          padding: "8px 12px",
        },
        head: {
          fontWeight: 600,
          fontSize: "0.75rem",
          textTransform: "uppercase",
          letterSpacing: "0.05em",
          color: "rgba(0, 0, 0, 0.5)",
        },
      },
    },
  },
};

const darkThemeOptions: ThemeOptions = {
  palette: {
    mode: "dark",
    primary: {
      main: "#60a5fa",
      light: "#93c5fd",
      dark: "#3b82f6",
      contrastText: "#000",
    },
    secondary: {
      main: "#a78bfa",
      light: "#c4b5fd",
      dark: "#7c3aed",
      contrastText: "#000",
    },
    success: { main: "#4ade80", light: "#86efac", dark: "#22c55e" },
    warning: { main: "#fb923c", light: "#fdba74", dark: "#f97316" },
    error: { main: "#f87171", light: "#fca5a5", dark: "#ef4444" },
    info: { main: "#22d3ee", light: "#67e8f9", dark: "#06b6d4" },
    background: {
      default: "#0f172a",
      paper: "rgba(30, 41, 59, 0.85)",
    },
    divider: "rgba(255, 255, 255, 0.06)",
  },
  typography: commonTypography,
  shape: { borderRadius: 12 },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: "none", fontWeight: 600, borderRadius: 8 },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
          backdropFilter: "blur(16px)",
          border: "1px solid rgba(255, 255, 255, 0.08)",
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 500, fontSize: "0.75rem" },
        sizeSmall: { height: 22 },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: "1px solid rgba(255, 255, 255, 0.04)",
          padding: "8px 12px",
        },
        head: {
          fontWeight: 600,
          fontSize: "0.75rem",
          textTransform: "uppercase",
          letterSpacing: "0.05em",
          color: "rgba(255, 255, 255, 0.4)",
        },
      },
    },
  },
};

export const lightTheme = createTheme(lightThemeOptions);
export const darkTheme = createTheme(darkThemeOptions);

export const getTheme = (mode: "light" | "dark") => {
  return mode === "light" ? lightTheme : darkTheme;
};
