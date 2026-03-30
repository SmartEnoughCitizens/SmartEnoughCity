import {
  Box,
  Chip,
  Paper,
  Stack,
  Typography,
  alpha,
  useTheme,
} from "@mui/material";
import type { TrafficRecommendation } from "@/types";

const humanize = (value: string) => value.replaceAll("_", " ");

interface TrafficRecommendationsProps {
  recommendations: TrafficRecommendation[];
  selectedRecommendationId: string | null;
  onSelectRecommendation: (recommendationId: string) => void;
}

export const TrafficRecommendations = ({
  recommendations,
  selectedRecommendationId,
  onSelectRecommendation,
}: TrafficRecommendationsProps) => {
  const theme = useTheme();

  return (
    <Box
      sx={{
        display: "grid",
        gap: 1.5,
        gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
      }}
    >
      {recommendations.map((recommendation) => {
        const selected =
          recommendation.recommendationId === selectedRecommendationId;
        const bestRoute = recommendation.alternativeRoutes[0];

        return (
          <Paper
            key={recommendation.recommendationId}
            elevation={0}
            onClick={() =>
              onSelectRecommendation(recommendation.recommendationId)
            }
            sx={{
              p: 2,
              borderRadius: 2,
              border: "1px solid",
              borderColor: selected
                ? theme.palette.primary.main
                : alpha(theme.palette.divider, 0.8),
              backgroundColor: selected
                ? alpha(theme.palette.primary.main, 0.08)
                : alpha(theme.palette.background.paper, 0.9),
              cursor: "pointer",
              transition: "border-color 0.2s ease, transform 0.2s ease",
              "&:hover": {
                borderColor: theme.palette.primary.main,
                transform: "translateY(-2px)",
              },
            }}
          >
            <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
              <Chip
                size="small"
                color={selected ? "primary" : "default"}
                label={`${recommendation.congestionLevel} congestion`}
              />
              <Chip
                size="small"
                variant="outlined"
                label={`${Math.round(recommendation.confidenceScore * 100)}% confidence`}
              />
            </Stack>

            <Typography variant="subtitle1" fontWeight={700} sx={{ mt: 1.5 }}>
              {recommendation.title}
            </Typography>

            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ mt: 0.75 }}
            >
              {recommendation.summary}
            </Typography>

            <Stack
              direction="row"
              spacing={1}
              useFlexGap
              flexWrap="wrap"
              sx={{ mt: 1.5 }}
            >
              <Chip
                size="small"
                label={`${humanize(recommendation.dayType)} · ${humanize(recommendation.timeSlot)}`}
              />
              <Chip
                size="small"
                label={`Volume ${recommendation.averageVolume.toFixed(0)}`}
              />
            </Stack>

            {bestRoute && (
              <Box
                sx={{
                  mt: 1.5,
                  p: 1.25,
                  borderRadius: 1.5,
                  backgroundColor: alpha(bestRoute.color, 0.12),
                }}
              >
                <Typography variant="caption" color="text.secondary">
                  Best alternative route
                </Typography>
                <Typography variant="body2" fontWeight={700}>
                  {bestRoute.label}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Save about {bestRoute.estimatedTimeSavingsMinutes} min ·{" "}
                  {bestRoute.distanceKm.toFixed(1)} km
                </Typography>
              </Box>
            )}

            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ mt: 1.5, display: "block" }}
            >
              {recommendation.recommendedAction}
            </Typography>
          </Paper>
        );
      })}
    </Box>
  );
};
