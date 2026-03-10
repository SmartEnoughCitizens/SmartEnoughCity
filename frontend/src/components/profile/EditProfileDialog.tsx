/**
 * Dialog for editing the current user's profile
 */

import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from "@mui/material";
import { isAxiosError } from "axios";
import { useGetProfile, useUpdateProfile } from "@/hooks";

const getErrorMessage = (error: Error | null): string => {
  if (!error) return "Failed to update profile";
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message;
  }
  return error.message;
};

interface EditProfileDialogProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export const EditProfileDialog = ({
  open,
  onClose,
  onSuccess,
}: EditProfileDialogProps) => {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");

  const profileQuery = useGetProfile();
  const updateMutation = useUpdateProfile();

  useEffect(() => {
    if (profileQuery.data) {
      setFirstName(profileQuery.data.firstName ?? "");
      setLastName(profileQuery.data.lastName ?? "");
      setEmail(profileQuery.data.email ?? "");
    }
  }, [profileQuery.data]);

  const resetForm = () => {
    if (profileQuery.data) {
      setFirstName(profileQuery.data.firstName ?? "");
      setLastName(profileQuery.data.lastName ?? "");
      setEmail(profileQuery.data.email ?? "");
    }
    updateMutation.reset();
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await updateMutation.mutateAsync({
        firstName,
        lastName,
        email,
      });
      onSuccess(response.message);
      handleClose();
    } catch {
      // Error displayed via mutation state
    }
  };

  const isFormValid = firstName.trim() && lastName.trim() && email.trim();

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Edit Profile</DialogTitle>
        <DialogContent
          sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}
        >
          {profileQuery.isLoading && (
            <CircularProgress size={24} sx={{ alignSelf: "center", mt: 1 }} />
          )}

          {updateMutation.isError && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {getErrorMessage(updateMutation.error)}
            </Alert>
          )}

          <TextField
            label="First Name"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            disabled={updateMutation.isPending || profileQuery.isLoading}
            required
            fullWidth
            margin="dense"
          />
          <TextField
            label="Last Name"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            disabled={updateMutation.isPending || profileQuery.isLoading}
            required
            fullWidth
            margin="dense"
          />
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={updateMutation.isPending || profileQuery.isLoading}
            required
            fullWidth
            margin="dense"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleClose} disabled={updateMutation.isPending}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={updateMutation.isPending || !isFormValid}
          >
            {updateMutation.isPending ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Save Changes"
            )}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};
