/**
 * Password strength indicator with animated reveal
 */

import { LinearProgress, Typography } from "@mui/material";
import { AnimatePresence, motion } from "framer-motion";

type SemanticColor = "error" | "warning" | "info" | "success";

const getStrength = (
  password: string,
): { score: number; label: string; color: SemanticColor } => {
  let score = 0;
  if (password.length >= 8) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;

  if (score <= 1) return { score, label: "Weak", color: "error" };
  if (score === 2) return { score, label: "Fair", color: "warning" };
  if (score === 3) return { score, label: "Good", color: "info" };
  return { score, label: "Strong", color: "success" };
};

interface PasswordStrengthIndicatorProps {
  password: string;
}

export const PasswordStrengthIndicator = ({
  password,
}: PasswordStrengthIndicatorProps) => {
  const { score, label, color } = getStrength(password);

  return (
    <AnimatePresence>
      {password.length > 0 && (
        <motion.div
          style={{ overflow: "hidden" }}
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          exit={{ opacity: 0, height: 0 }}
          transition={{ duration: 0.2 }}
        >
          <LinearProgress
            color={color}
            variant="determinate"
            value={(Math.max(score, 1) / 4) * 100}
            sx={{ mt: 1, borderRadius: 1 }}
          />
          <Typography
            variant="caption"
            color={`${color}.main`}
            sx={{ mt: 0.5, display: "block" }}
          >
            {label}
          </Typography>
        </motion.div>
      )}
    </AnimatePresence>
  );
};
