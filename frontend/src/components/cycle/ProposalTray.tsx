/**
 * ProposalTray — compact floating widget for City_Manager / Cycle_Admin.
 * Collapsed: shows only the pending proposal count badge.
 * Expanded: shows the full proposal list for selection/review.
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
  Paper,
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

export const ProposalTray = ({
  proposals,
  onSelect,
  activeProposalId,
}: ProposalTrayProps) => {
  const [open, setOpen] = useState(false);

  if (proposals.length === 0) return null;

  return (
    <Paper
      elevation={0}
      sx={{
        borderRadius: 2,
        border: "1px solid rgba(251,191,36,0.5)",
        bgcolor: "background.paper",
        overflow: "hidden",
        width: open ? 240 : "fit-content",
        transition: "width 0.2s ease",
      }}
    >
      {/* Collapsed / header row */}
      <Box
        onClick={() => setOpen((v) => !v)}
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1,
          px: 1.25,
          py: 0.75,
          cursor: "pointer",
          "&:hover": { bgcolor: "rgba(251,191,36,0.08)" },
        }}
      >
        <AssignmentIcon
          sx={{ fontSize: "0.95rem", color: "#fbbf24", flexShrink: 0 }}
        />
        {/* Count badge — always visible */}
        <Chip
          label={proposals.length}
          size="small"
          sx={{
            height: 20,
            minWidth: 20,
            fontSize: "0.65rem",
            fontWeight: 700,
            bgcolor: "#fbbf24",
            color: "#000",
            flexShrink: 0,
          }}
        />
        <Typography
          variant="caption"
          fontWeight={700}
          sx={{
            color: "#fbbf24",
            fontSize: "0.7rem",
            flex: 1,
            whiteSpace: "nowrap",
          }}
        >
          Pending Proposals
        </Typography>
        {open ? (
          <ExpandLessIcon sx={{ fontSize: "0.9rem", color: "#fbbf24" }} />
        ) : (
          <ExpandMoreIcon sx={{ fontSize: "0.9rem", color: "#fbbf24" }} />
        )}
      </Box>

      {/* Expanded list */}
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
                  px: 1.25,
                  borderTop: "1px solid rgba(255,255,255,0.05)",
                  "&.Mui-selected": { bgcolor: "rgba(251,191,36,0.15)" },
                  "&:hover": { bgcolor: "rgba(251,191,36,0.1)" },
                }}
              >
                <ListItemIcon sx={{ minWidth: 24 }}>
                  <Box
                    sx={{
                      width: 7,
                      height: 7,
                      borderRadius: "50%",
                      bgcolor: isActive ? "#fbbf24" : "#94a3b8",
                    }}
                  />
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography
                      variant="caption"
                      fontWeight={600}
                      sx={{ fontSize: "0.68rem" }}
                    >
                      {p.submittedBy}
                    </Typography>
                  }
                  secondary={
                    <Typography
                      variant="caption"
                      sx={{ fontSize: "0.6rem", color: "text.disabled" }}
                    >
                      {p.stationCount} stn · {p.improvedAreaCount} area · {date}
                    </Typography>
                  }
                />
                {isActive && (
                  <Chip
                    label="Active"
                    size="small"
                    sx={{
                      height: 14,
                      fontSize: "0.55rem",
                      bgcolor: "rgba(251,191,36,0.25)",
                      color: "#fbbf24",
                    }}
                  />
                )}
              </ListItemButton>
            );
          })}
        </List>
      </Collapse>
    </Paper>
  );
};
