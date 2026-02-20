/**
 * Dialog form for registering a new user
 */

import { useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  MenuItem,
  Alert,
  CircularProgress,
} from "@mui/material";
import { isAxiosError } from "axios";
import { useRegisterUser } from "@/hooks";
import { ALLOWED_ROLES } from "@/types";

const getErrorMessage = (error: Error | null): string => {
  if (!error) return "Failed to register user";
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message;
  }
  return error.message;
};

interface AddUserDialogProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export const AddUserDialog = ({
  open,
  onClose,
  onSuccess,
}: AddUserDialogProps) => {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [role, setRole] = useState("");
  const [password, setPassword] = useState("");

  const registerMutation = useRegisterUser();

  const resetForm = () => {
    setUsername("");
    setEmail("");
    setFirstName("");
    setLastName("");
    setRole("");
    setPassword("");
    registerMutation.reset();
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const response = await registerMutation.mutateAsync({
        username,
        email,
        firstName,
        lastName,
        role,
        ...(password && { password }),
      });
      onSuccess(response.message);
      handleClose();
    } catch {
      // Error displayed via mutation state
    }
  };

  const isFormValid =
    username.trim() && email.trim() && firstName.trim() && lastName.trim() && role;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Add New User</DialogTitle>
        <DialogContent sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
          {registerMutation.isError && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {getErrorMessage(registerMutation.error)}
            </Alert>
          )}

          <TextField
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={registerMutation.isPending}
            required
            fullWidth
            margin="dense"
          />
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={registerMutation.isPending}
            required
            fullWidth
            margin="dense"
          />
          <TextField
            label="First Name"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            disabled={registerMutation.isPending}
            required
            fullWidth
            margin="dense"
          />
          <TextField
            label="Last Name"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            disabled={registerMutation.isPending}
            required
            fullWidth
            margin="dense"
          />
          <TextField
            label="Role"
            select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            disabled={registerMutation.isPending}
            required
            fullWidth
            margin="dense"
          >
            {ALLOWED_ROLES.map((r) => (
              <MenuItem key={r} value={r}>
                {r.replace("_", " ")}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Password (optional)"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={registerMutation.isPending}
            fullWidth
            margin="dense"
            helperText="Leave empty to set temporary password (ChangeMe@123)"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleClose} disabled={registerMutation.isPending}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={registerMutation.isPending || !isFormValid}
          >
            {registerMutation.isPending ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Add User"
            )}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};
