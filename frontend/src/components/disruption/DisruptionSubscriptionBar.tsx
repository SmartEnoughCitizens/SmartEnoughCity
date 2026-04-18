import { useState } from "react";
import {
  Box,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Button,
  Typography,
  CircularProgress,
} from "@mui/material";
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import NotificationsOffIcon from "@mui/icons-material/NotificationsOff";
import {
  useDisruptionSubscriptions,
  useSubscribeToMode,
  useUnsubscribeFromMode,
} from "@/hooks";

const SUBSCRIPTION_MODES: { key: string; label: string }[] = [
  { key: "BUS", label: "Bus" },
  { key: "TRAM", label: "Tram" },
  { key: "TRAIN", label: "Train" },
  { key: "CYCLE", label: "Cycle" },
];

const BLUE = "#3B82F6";

export function DisruptionSubscriptionBar() {
  const { data: subscribedModes = [], isLoading } =
    useDisruptionSubscriptions();
  const subscribeMutation = useSubscribeToMode();
  const unsubscribeMutation = useUnsubscribeFromMode();

  const [confirmMode, setConfirmMode] = useState<string | null>(null);
  const [confirmAction, setConfirmAction] = useState<"subscribe" | "unsubscribe">("subscribe");

  const isPending = subscribeMutation.isPending || unsubscribeMutation.isPending;

  const handleChipClick = (mode: string) => {
    if (isPending) return;
    if (subscribedModes.includes(mode)) {
      setConfirmAction("unsubscribe");
    } else {
      setConfirmAction("subscribe");
    }
    setConfirmMode(mode);
  };

  const handleConfirm = () => {
    if (!confirmMode) return;
    const mutation = confirmAction === "subscribe" ? subscribeMutation : unsubscribeMutation;
    mutation.mutate(confirmMode, { onSettled: () => setConfirmMode(null) });
  };

  const confirmLabel = SUBSCRIPTION_MODES.find((m) => m.key === confirmMode)?.label ?? confirmMode;

  return (
    <>
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1,
          px: 1.5,
          py: 0.75,
          flexWrap: "wrap",
        }}
      >
        <Typography
          variant="caption"
          sx={{ color: "text.secondary", fontWeight: 500, mr: 0.5 }}
        >
          Alert subscriptions:
        </Typography>

        {isLoading ? (
          <CircularProgress size={14} />
        ) : (
          SUBSCRIPTION_MODES.map(({ key, label }) => {
            const subscribed = subscribedModes.includes(key);
            return (
              <Chip
                key={key}
                size="small"
                label={label}
                icon={
                  subscribed ? (
                    <NotificationsActiveIcon sx={{ fontSize: "0.85rem !important" }} />
                  ) : (
                    <NotificationsOffIcon sx={{ fontSize: "0.85rem !important" }} />
                  )
                }
                onClick={() => handleChipClick(key)}
                sx={{
                  fontSize: "0.7rem",
                  height: 22,
                  cursor: isPending ? "not-allowed" : "pointer",
                  backgroundColor: subscribed ? `${BLUE}22` : "transparent",
                  color: subscribed ? BLUE : "text.disabled",
                  border: `1px solid ${subscribed ? BLUE : "rgba(255,255,255,0.15)"}`,
                  "& .MuiChip-icon": { color: "inherit" },
                  "&:hover": {
                    backgroundColor: subscribed
                      ? `${BLUE}33`
                      : "rgba(255,255,255,0.06)",
                  },
                  transition: "all 0.15s ease",
                }}
              />
            );
          })
        )}
      </Box>

      <Dialog
        open={confirmMode !== null}
        onClose={() => setConfirmMode(null)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ pb: 1 }}>
          {confirmAction === "subscribe" ? "Subscribe to alerts?" : "Unsubscribe from alerts?"}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            {confirmAction === "subscribe"
              ? `You'll receive email alerts when a ${confirmLabel} disruption is reported. You can unsubscribe at any time from this panel or via the link in any alert email.`
              : `You'll stop receiving email alerts for ${confirmLabel} disruptions.`}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmMode(null)} size="small">
            Cancel
          </Button>
          <Button
            variant="contained"
            size="small"
            onClick={handleConfirm}
            disabled={subscribeMutation.isPending || unsubscribeMutation.isPending}
            sx={{
              backgroundColor: BLUE,
              "&:hover": { backgroundColor: BLUE, opacity: 0.85 },
            }}
          >
            {(subscribeMutation.isPending || unsubscribeMutation.isPending) ? (
              <CircularProgress size={14} sx={{ color: "inherit" }} />
            ) : confirmAction === "subscribe" ? (
              "Subscribe"
            ) : (
              "Unsubscribe"
            )}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
