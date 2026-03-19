/**
 * Tram dashboard — placeholder until tram data integration is complete
 */

import { Box, Typography } from "@mui/material";
import TramIcon from "@mui/icons-material/Tram";

export const TramDashboard = () => (
  <Box
    sx={{
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      height: "100%",
      gap: 2,
      color: "text.secondary",
    }}
  >
    <TramIcon sx={{ fontSize: 64, opacity: 0.4 }} />
    <Typography variant="h5">Tram Dashboard</Typography>
    <Typography variant="body2">Coming soon — tram data integration in progress.</Typography>
  </Box>
);
