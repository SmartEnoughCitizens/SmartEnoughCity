/**
 * MUI theme configuration - Map-centric design
 * Glass-panel aesthetic with semi-transparent overlays
 */

import { createTheme, type ThemeOptions } from "@mui/material/styles";

// ─── Colour constants ────────────────────────────────────────────────────────

// Light primary
const LIGHT_PRIMARY_MAIN = "#2563eb";
const LIGHT_PRIMARY_LIGHT = "#60a5fa";
const LIGHT_PRIMARY_DARK = "#1d4ed8";

// Light secondary
const LIGHT_SECONDARY_MAIN = "#7c3aed";
const LIGHT_SECONDARY_LIGHT = "#a78bfa";
const LIGHT_SECONDARY_DARK = "#5b21b6";

// Light status
const LIGHT_SUCCESS_MAIN = "#16a34a";
const LIGHT_SUCCESS_LIGHT = "#4ade80";
const LIGHT_SUCCESS_DARK = "#15803d";
const LIGHT_WARNING_MAIN = "#ea580c";
const LIGHT_WARNING_LIGHT = "#fb923c";
const LIGHT_WARNING_DARK = "#c2410c";
const LIGHT_ERROR_MAIN = "#dc2626";
const LIGHT_ERROR_LIGHT = "#f87171";
const LIGHT_ERROR_DARK = "#b91c1c";
const LIGHT_INFO_MAIN = "#0891b2";
const LIGHT_INFO_LIGHT = "#22d3ee";
const LIGHT_INFO_DARK = "#0e7490";

// Light background / surfaces
const LIGHT_BG_DEFAULT = "#f1f5f9";
const LIGHT_BG_PAPER = "rgba(255, 255, 255, 0.92)";
const LIGHT_DIVIDER = "rgba(0, 0, 0, 0.06)";
const LIGHT_PAPER_BORDER = "rgba(0, 0, 0, 0.06)";
const LIGHT_TABLE_CELL_BORDER = "rgba(0, 0, 0, 0.04)";
const LIGHT_TABLE_HEAD_COLOR = "rgba(0, 0, 0, 0.5)";

// Dark primary
const DARK_PRIMARY_MAIN = "#60a5fa";
const DARK_PRIMARY_LIGHT = "#93c5fd";
const DARK_PRIMARY_DARK = "#3b82f6";

// Dark secondary
const DARK_SECONDARY_MAIN = "#a78bfa";
const DARK_SECONDARY_LIGHT = "#c4b5fd";
const DARK_SECONDARY_DARK = "#7c3aed";

// Dark status
const DARK_SUCCESS_MAIN = "#4ade80";
const DARK_SUCCESS_LIGHT = "#86efac";
const DARK_SUCCESS_DARK = "#22c55e";
const DARK_WARNING_MAIN = "#fb923c";
const DARK_WARNING_LIGHT = "#fdba74";
const DARK_WARNING_DARK = "#f97316";
const DARK_ERROR_MAIN = "#f87171";
const DARK_ERROR_LIGHT = "#fca5a5";
const DARK_ERROR_DARK = "#ef4444";
const DARK_INFO_MAIN = "#22d3ee";
const DARK_INFO_LIGHT = "#67e8f9";
const DARK_INFO_DARK = "#06b6d4";

// Dark background / surfaces
const DARK_BG_DEFAULT = "#0f172a";
const DARK_BG_PAPER = "rgba(30, 41, 59, 0.85)";
const DARK_DIVIDER = "rgba(255, 255, 255, 0.06)";
const DARK_PAPER_BORDER = "rgba(255, 255, 255, 0.08)";
const DARK_TABLE_CELL_BORDER = "rgba(255, 255, 255, 0.04)";
const DARK_TABLE_HEAD_COLOR = "rgba(255, 255, 255, 0.4)";

// ─── Shared typography ───────────────────────────────────────────────────────

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

// ─── Light theme ─────────────────────────────────────────────────────────────

const lightThemeOptions: ThemeOptions = {
  palette: {
    mode: "light",
    primary: {
      main: LIGHT_PRIMARY_MAIN,
      light: LIGHT_PRIMARY_LIGHT,
      dark: LIGHT_PRIMARY_DARK,
      contrastText: "#fff",
    },
    secondary: {
      main: LIGHT_SECONDARY_MAIN,
      light: LIGHT_SECONDARY_LIGHT,
      dark: LIGHT_SECONDARY_DARK,
      contrastText: "#fff",
    },
    success: { main: LIGHT_SUCCESS_MAIN, light: LIGHT_SUCCESS_LIGHT, dark: LIGHT_SUCCESS_DARK },
    warning: { main: LIGHT_WARNING_MAIN, light: LIGHT_WARNING_LIGHT, dark: LIGHT_WARNING_DARK },
    error: { main: LIGHT_ERROR_MAIN, light: LIGHT_ERROR_LIGHT, dark: LIGHT_ERROR_DARK },
    info: { main: LIGHT_INFO_MAIN, light: LIGHT_INFO_LIGHT, dark: LIGHT_INFO_DARK },
    background: {
      default: LIGHT_BG_DEFAULT,
      paper: LIGHT_BG_PAPER,
    },
    divider: LIGHT_DIVIDER,
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
          border: `1px solid ${LIGHT_PAPER_BORDER}`,
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
          borderBottom: `1px solid ${LIGHT_TABLE_CELL_BORDER}`,
          padding: "8px 12px",
        },
        head: {
          fontWeight: 600,
          fontSize: "0.75rem",
          textTransform: "uppercase",
          letterSpacing: "0.05em",
          color: LIGHT_TABLE_HEAD_COLOR,
        },
      },
    },
  },
};

// ─── Dark theme ──────────────────────────────────────────────────────────────

const darkThemeOptions: ThemeOptions = {
  palette: {
    mode: "dark",
    primary: {
      main: DARK_PRIMARY_MAIN,
      light: DARK_PRIMARY_LIGHT,
      dark: DARK_PRIMARY_DARK,
      contrastText: "#000",
    },
    secondary: {
      main: DARK_SECONDARY_MAIN,
      light: DARK_SECONDARY_LIGHT,
      dark: DARK_SECONDARY_DARK,
      contrastText: "#000",
    },
    success: { main: DARK_SUCCESS_MAIN, light: DARK_SUCCESS_LIGHT, dark: DARK_SUCCESS_DARK },
    warning: { main: DARK_WARNING_MAIN, light: DARK_WARNING_LIGHT, dark: DARK_WARNING_DARK },
    error: { main: DARK_ERROR_MAIN, light: DARK_ERROR_LIGHT, dark: DARK_ERROR_DARK },
    info: { main: DARK_INFO_MAIN, light: DARK_INFO_LIGHT, dark: DARK_INFO_DARK },
    background: {
      default: DARK_BG_DEFAULT,
      paper: DARK_BG_PAPER,
    },
    divider: DARK_DIVIDER,
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
          border: `1px solid ${DARK_PAPER_BORDER}`,
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
          borderBottom: `1px solid ${DARK_TABLE_CELL_BORDER}`,
          padding: "8px 12px",
        },
        head: {
          fontWeight: 600,
          fontSize: "0.75rem",
          textTransform: "uppercase",
          letterSpacing: "0.05em",
          color: DARK_TABLE_HEAD_COLOR,
        },
      },
    },
  },
};

// ─── Exports ─────────────────────────────────────────────────────────────────

export const lightTheme = createTheme(lightThemeOptions);
export const darkTheme = createTheme(darkThemeOptions);

export const getTheme = (mode: "light" | "dark") => {
  return mode === "light" ? lightTheme : darkTheme;
};
