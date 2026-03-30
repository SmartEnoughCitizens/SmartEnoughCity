/**
 * Login page — split layout: branding left, form right
 */

import { Alert, Box, Paper, Typography } from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { alpha } from "@mui/material/styles";
import { motion, useReducedMotion } from "framer-motion";
import { useLocation } from "react-router-dom";
import { LoginForm } from "@/components/auth/LoginForm";

const orbs = [
  { size: 400, top: "-15%", left: "-10%", color: "primary", duration: 22 },
  { size: 300, top: "55%",  left: "55%",  color: "secondary", duration: 28 },
  { size: 350, top: "30%",  left: "30%",  color: "primary",   duration: 20 },
  { size: 250, top: "70%",  left: "-5%",  color: "secondary", duration: 26 },
];

export const LoginPage = () => {
  const location = useLocation();
  const successMessage = (location.state as { successMessage?: string } | null)
    ?.successMessage;

  const muiTheme = useTheme();
  const shouldReduceMotion = useReducedMotion();

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>

      {/* ── Left panel: branding (white) ─────────────────────────────────── */}
      <Box
        sx={{
          display: { xs: "none", md: "flex" },
          width: "50%",
          position: "relative",
          overflow: "hidden",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          bgcolor: "background.paper",
        }}
      >
        {/* Subtle orbs on white */}
        {orbs.map((orb, i) => (
          <motion.div
            key={i}
            style={{
              position: "absolute",
              width: orb.size,
              height: orb.size,
              borderRadius: "50%",
              top: orb.top,
              left: orb.left,
              filter: "blur(60px)",
              willChange: "transform",
              backgroundColor:
                orb.color === "primary"
                  ? alpha(muiTheme.palette.primary.main, 0.07)
                  : alpha(muiTheme.palette.secondary.main, 0.07),
              zIndex: 0,
            }}
            {...(!shouldReduceMotion && {
              animate: {
                x: [0, 25, -15, 8, 0],
                y: [0, -20, 12, -8, 0],
              },
              transition: {
                duration: orb.duration,
                repeat: Infinity,
                ease: "easeInOut",
              },
            })}
          />
        ))}

        {/* Branding */}
        <motion.div
          style={{ position: "relative", zIndex: 1, textAlign: "center" }}
          initial={{ opacity: 0, y: shouldReduceMotion ? 0 : 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
        >
          <Box sx={{ display: "flex", justifyContent: "center", mb: 3 }}>
            <img src="/favicon.svg" height={400} alt="SmartEnoughCity" />
          </Box>
          <Typography
            variant="h3"
            component="h1"
            fontWeight={700}
            color="text.primary"
            sx={{ mb: 1 }}
          >
            SmartEnoughCity
          </Typography>
          <Typography color="text.secondary" sx={{ fontSize: "1.05rem" }}>
            Transport Analytics Dashboard
          </Typography>
        </motion.div>
      </Box>

      {/* ── Right panel: form (cyan) ─────────────────────────────────────── */}
      <Box
        sx={{
          flex: 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: `linear-gradient(135deg, ${muiTheme.palette.primary.dark} 0%, ${muiTheme.palette.primary.main} 100%)`,
          minHeight: "100vh",
          px: 2,
        }}
      >
        {successMessage && (
          <Box
            sx={{
              position: "fixed",
              top: 16,
              left: "50%",
              transform: "translateX(-50%)",
              zIndex: 9999,
              width: "100%",
              maxWidth: 400,
              px: 2,
            }}
          >
            <Alert severity="success">{successMessage}</Alert>
          </Box>
        )}
        <Paper
          elevation={4}
          sx={{ p: 4, maxWidth: 420, width: "100%", borderRadius: 3 }}
        >
          <LoginForm />
        </Paper>
      </Box>
    </Box>
  );
};
