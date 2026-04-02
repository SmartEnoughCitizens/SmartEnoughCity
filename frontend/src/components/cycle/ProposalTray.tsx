/**
 * ProposalTray — shows pending station proposals to City_Manager / Cycle_Admin.
 * Renders inside the Coverage side-panel. Clicking a proposal loads it for review.
 */

import { useState } from "react";
import {
  Box,
  Typography,
  Chip,
  Collapse,
  List,
  ListItemButton,
  ListItemText,
  ListItemIcon,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import AssignmentIcon from "@mui/icons-material/Assignment";
import type { StationProposalSummary } from "@/types";

interface ProposalTrayProps {
  proposals: StationProposalSummary[];
  onSelect: (proposal: StationProposalSummary) => void;
  activeProposalId?: number | null;
}

export const ProposalTray = ({ proposals, onSelect, activeProposalId }: ProposalTrayProps) => {
  const [open, setOpen] = useState(true);

  if (proposals.length === 0) return null;

  return (
    <Box
      sx={{
        mx: 1,
        mb: 1,
        borderRadius: 2,
        border: "1px solid rgba(251,191,36,0.6)",
        bgcolor: "background.paper",
        overflow: "hidden",
      }}
    >
      {/* Header */}
      <Box
        onClick={() => setOpen((v) => !v)}
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1,
          px: 1.5,
          py: 0.75,
          cursor: "pointer",
          "&:hover": { bgcolor: "rgba(251,191,36,0.08)" },
        }}
      >
        <AssignmentIcon sx={{ fontSize: "0.9rem", color: "#fbbf24" }} />
        <Typography variant="caption" fontWeight={700} sx={{ color: "#fbbf24", fontSize: "0.72rem", flex: 1 }}>
          Pending Proposals
        </Typography>
        <Chip
          label={proposals.length}
          size="small"
          sx={{
            height: 18,
            fontSize: "0.62rem",
            fontWeight: 700,
            bgcolor: "#fbbf24",
            color: "#000",
          }}
        />
        {open ? (
          <ExpandLessIcon sx={{ fontSize: "0.9rem", color: "#fbbf24" }} />
        ) : (
          <ExpandMoreIcon sx={{ fontSize: "0.9rem", color: "#fbbf24" }} />
        )}
      </Box>

      {/* List */}
      <Collapse in={open}>
        <List disablePadding dense>
          {proposals.map((p) => {
            const isActive = p.id === activeProposalId;
            const date = new Date(p.submittedAt).toLocaleDateString("en-IE", {
              day: "numeric",
              month: "short",
              hour: "2-digit",
              minute: "2-digit",
            });
            return (
              <ListItemButton
                key={p.id}
                onClick={() => onSelect(p)}
                selected={isActive}
                sx={{
                  py: 0.5,
                  px: 1.5,
                  borderTop: "1px solid rgba(255,255,255,0.05)",
                  "&.Mui-selected": { bgcolor: "rgba(251,191,36,0.15)" },
                  "&:hover": { bgcolor: "rgba(251,191,36,0.1)" },
                }}
              >
                <ListItemIcon sx={{ minWidth: 28 }}>
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      borderRadius: "50%",
                      bgcolor: isActive ? "#fbbf24" : "#94a3b8",
                    }}
                  />
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="caption" fontWeight={600} sx={{ fontSize: "0.68rem" }}>
                      {p.submittedBy}
                      <Typography
                        component="span"
                        variant="caption"
                        sx={{ ml: 0.5, fontSize: "0.62rem", color: "text.secondary" }}
                      >
                        · {p.stationCount} station{p.stationCount !== 1 ? "s" : ""} · {p.improvedAreaCount} area{p.improvedAreaCount !== 1 ? "s" : ""}
                      </Typography>
                    </Typography>
                  }
                  secondary={
                    <Typography variant="caption" sx={{ fontSize: "0.6rem", color: "text.disabled" }}>
                      {date}
                    </Typography>
                  }
                />
                {isActive && (
                  <Chip
                    label="Reviewing"
                    size="small"
                    sx={{ height: 16, fontSize: "0.58rem", bgcolor: "rgba(251,191,36,0.25)", color: "#fbbf24" }}
                  />
                )}
              </ListItemButton>
            );
          })}
        </List>
      </Collapse>
    </Box>
  );
};
