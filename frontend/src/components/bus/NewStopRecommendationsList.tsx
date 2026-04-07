import { Box, CircularProgress, Typography } from "@mui/material";
import { useBusNewStopRecommendations } from "@/hooks";

export const NewStopRecommendationsList = () => {
  const { data = [], isLoading, isError } = useBusNewStopRecommendations();

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
        {data.map((row, idx) => (
          <Box component="tr" key={`${row.routeId}-${row.stopA.id}-${row.stopB.id}`}>
            <Box component="td" sx={{ p: "6px 6px", opacity: 0.6, verticalAlign: "top" }}>
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
        ))}
      </Box>
    </Box>
  );
};
