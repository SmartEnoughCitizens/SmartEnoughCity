import { Box, Chip, CircularProgress, Typography } from "@mui/material";
import { useBusNewStopRecommendations } from "@/hooks";
import type { BusNewStopRecommendation } from "@/types";

const recommendationKey = (row: BusNewStopRecommendation) =>
  `${row.routeId}-${row.stopA.id}-${row.stopB.id}`;

type Props = {
  selectedRecommendation: BusNewStopRecommendation | null;
  onSelectRecommendation: (row: BusNewStopRecommendation | null) => void;
};

export const NewStopRecommendationsList = ({
  selectedRecommendation,
  onSelectRecommendation,
}: Props) => {
  const { data = [], isLoading, isError } = useBusNewStopRecommendations();

  const selectedKey = selectedRecommendation
    ? recommendationKey(selectedRecommendation)
    : null;

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  if (isError) {
    return (
      <Typography variant="body2" sx={{ opacity: 0.6, py: 1 }}>
        Could not load recommendations. Ensure the materialized view is
        deployed.
      </Typography>
    );
  }

  if (data.length === 0) {
    return (
      <Typography variant="body2" sx={{ opacity: 0.6, py: 1 }}>
        No recommendations available.
      </Typography>
    );
  }

  return (
    <Box
      component="table"
      sx={{ width: "100%", borderCollapse: "collapse", fontSize: "0.75rem" }}
    >
      <Box component="thead">
        <Box component="tr">
          <Box
            component="th"
            sx={{ textAlign: "left", p: "4px 6px", opacity: 0.7, width: 28 }}
          >
            #
          </Box>
          <Box
            component="th"
            sx={{ textAlign: "left", p: "4px 6px", opacity: 0.7 }}
          >
            Route
          </Box>
          <Box
            component="th"
            sx={{ textAlign: "left", p: "4px 6px", opacity: 0.7 }}
          >
            Between stops
          </Box>
          <Box
            component="th"
            sx={{ textAlign: "right", p: "4px 6px", opacity: 0.7 }}
          >
            Score
          </Box>
        </Box>
      </Box>
      <Box component="tbody">
        {data.map((row, idx) => {
          const key = recommendationKey(row);
          const isSelected = key === selectedKey;
          return (
            <Box
              component="tr"
              key={key}
              onClick={() => onSelectRecommendation(isSelected ? null : row)}
              sx={{
                cursor: "pointer",
                bgcolor: isSelected ? "action.selected" : "transparent",
                transition: "background-color 0.15s ease",
                "&:hover": {
                  bgcolor: isSelected ? "action.selected" : "action.hover",
                },
              }}
            >
              <Box
                component="td"
                sx={{ p: "6px 6px", opacity: 0.6, verticalAlign: "top" }}
              >
                {idx + 1}
              </Box>
              <Box component="td" sx={{ p: "6px 6px", verticalAlign: "top" }}>
                <strong>{row.routeShortName}</strong>
                <Typography
                  component="span"
                  variant="caption"
                  display="block"
                  sx={{ opacity: 0.65, lineHeight: 1.25 }}
                >
                  {row.routeLongName}
                </Typography>
              </Box>
              <Box component="td" sx={{ p: "6px 6px", verticalAlign: "top" }}>
                <Typography variant="inherit" sx={{ lineHeight: 1.35 }}>
                  {row.stopA.name}{" "}
                  <Box component="span" sx={{ opacity: 0.65 }}>
                    ({row.stopA.code})
                  </Box>
                </Typography>
                <Typography
                  variant="inherit"
                  sx={{ opacity: 0.5, fontSize: "0.65rem", my: 0.15 }}
                >
                  →
                </Typography>
                <Typography variant="inherit" sx={{ lineHeight: 1.35 }}>
                  {row.stopB.name}{" "}
                  <Box component="span" sx={{ opacity: 0.65 }}>
                    ({row.stopB.code})
                  </Box>
                </Typography>
              </Box>
              <Box
                component="td"
                sx={{
                  p: "6px 6px",
                  textAlign: "right",
                  verticalAlign: "top",
                  fontWeight: 600,
                  whiteSpace: "nowrap",
                }}
              >
                {row.combinedScore.toFixed(2)}
              </Box>
            </Box>
          );
        })}
      </Box>
    </Box>
  );
};

export const RecommendationScoreCard = ({
  recommendation,
}: {
  recommendation: BusNewStopRecommendation;
}) => (
  <Box
    sx={{
      mt: 1,
      mb: 1,
      p: "8px 10px",
      borderRadius: 1,
      bgcolor: "action.hover",
      fontSize: "0.75rem",
    }}
  >
    <Typography
      variant="caption"
      sx={{ opacity: 0.6, display: "block", mb: 0.5 }}
    >
      Score breakdown
    </Typography>
    <Box sx={{ display: "flex", justifyContent: "space-between", mb: 0.5 }}>
      <Box>
        <Typography variant="caption" sx={{ opacity: 0.7 }}>
          Population density
        </Typography>
        <Typography
          variant="caption"
          sx={{ opacity: 0.5, display: "block", lineHeight: 1.2 }}
        >
          Nearby residents without close stops
        </Typography>
      </Box>
      <Typography variant="caption" sx={{ fontWeight: 600, ml: 1 }}>
        {recommendation.populationScore?.toFixed(2) ?? "—"}
      </Typography>
    </Box>
    <Box sx={{ display: "flex", justifyContent: "space-between", mb: 0.5 }}>
      <Box>
        <Typography variant="caption" sx={{ opacity: 0.7 }}>
          Public space proximity
        </Typography>
        <Typography
          variant="caption"
          sx={{ opacity: 0.5, display: "block", lineHeight: 1.2 }}
        >
          Parks, amenities and footfall generators nearby
        </Typography>
      </Box>
      <Typography variant="caption" sx={{ fontWeight: 600, ml: 1 }}>
        {recommendation.publicSpaceScore?.toFixed(2) ?? "—"}
      </Typography>
    </Box>
    <Box
      sx={{
        display: "flex",
        justifyContent: "space-between",
        pt: 0.5,
        borderTop: "1px solid",
        borderColor: "divider",
      }}
    >
      <Typography variant="caption" sx={{ fontWeight: 600 }}>
        Combined score
      </Typography>
      <Typography variant="caption" sx={{ fontWeight: 700 }}>
        {recommendation.combinedScore.toFixed(2)}
      </Typography>
    </Box>
  </Box>
);

export const SelectedRecommendationChip = ({
  recommendation,
  onClear,
}: {
  recommendation: BusNewStopRecommendation;
  onClear: () => void;
}) => (
  <Chip
    size="small"
    color="primary"
    variant="outlined"
    label={`Route: ${recommendation.routeShortName} · score ${recommendation.combinedScore.toFixed(2)}`}
    onDelete={onClear}
    sx={{ maxWidth: "100%", "& .MuiChip-label": { overflow: "hidden" } }}
  />
);
