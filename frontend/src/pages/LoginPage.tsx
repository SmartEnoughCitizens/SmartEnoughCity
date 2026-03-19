/**
 * Login page
 */

import { Alert, Box } from "@mui/material";
import { useLocation } from "react-router-dom";
import { LoginForm } from "@/components/auth/LoginForm";

export const LoginPage = () => {
  const location = useLocation();
  const successMessage = (location.state as { successMessage?: string } | null)
    ?.successMessage;

  return (
    <>
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
      <LoginForm />
    </>
  );
};
