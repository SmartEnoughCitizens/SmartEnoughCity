/**
 * PublicDisruptionPage — No-auth public page, accessible via QR code.
 * Shows the disruption on a map alongside a list of nearby alternatives
 * with walking links, so passengers can immediately navigate.
 */

import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Box,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  Link,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import TrainIcon from "@mui/icons-material/Train";
import TramIcon from "@mui/icons-material/Tram";
import PedalBikeIcon from "@mui/icons-material/PedalBike";
import EventIcon from "@mui/icons-material/Event";
import TrafficIcon from "@mui/icons-material/Traffic";
import CompareArrowsIcon from "@mui/icons-material/CompareArrows";
import DirectionsWalkIcon from "@mui/icons-material/DirectionsWalk";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { dashboardApi } from "@/api";
import type {
  DisruptionAlternative,
  DisruptionCause,
  DisruptionSeverity,
} from "@/types";

// Fix Leaflet default icon paths in bundled apps
delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)
  ._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

const SEVERITY_COLORS: Record<DisruptionSeverity, string> = {
  LOW: "#10B981",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
  CRITICAL: "#7C3AED",
};

const MODE_COLORS: Record<string, string> = {
  bus: "#3B82F6",
  rail: "#8B5CF6",
  tram: "#10B981",
  bike: "#F59E0B",
};

const TYPE_LABELS: Record<string, string> = {
  DELAY: "Service Delay",
  TRAM_DISRUPTION: "Tram Disruption",
  CONGESTION: "Traffic Congestion",
  CANCELLATION: "Cancellation",
  CONSTRUCTION: "Construction",
  EVENT: "Service Pressure",
  ACCIDENT: "Accident",
};

const CAUSE_ICONS: Record<string, React.ReactNode> = {
  EVENT: <EventIcon sx={{ fontSize: 18 }} />,
  CONGESTION: <TrafficIcon sx={{ fontSize: 18 }} />,
  CROSS_MODE: <CompareArrowsIcon sx={{ fontSize: 18 }} />,
};

const CONFIDENCE_COLORS: Record<string, string> = {
  HIGH: "#EF4444",
  MEDIUM: "#F59E0B",
  LOW: "#10B981",
};

function modeIcon(mode: string): React.ReactNode {
  const m = mode.toLowerCase();
  if (m.includes("bus")) return <DirectionsBusIcon sx={{ fontSize: 18 }} />;
  if (m.includes("bike") || m.includes("cycle"))
    return <PedalBikeIcon sx={{ fontSize: 18 }} />;
  if (m.includes("tram")) return <TramIcon sx={{ fontSize: 18 }} />;
  return <TrainIcon sx={{ fontSize: 18 }} />;
}

function modeColor(mode: string): string {
  return MODE_COLORS[mode.toLowerCase()] ?? "#6B7280";
}

function makeDisruptionIcon(color: string): L.DivIcon {
  return L.divIcon({
    className: "",
    html: `<div style="
      width:20px;height:20px;border-radius:50%;
      background:${color};border:3px solid white;
      box-shadow:0 0 0 2px ${color};
    "></div>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
  });
}

function makeAltIcon(color: string): L.DivIcon {
  return L.divIcon({
    className: "",
    html: `<div style="
      width:14px;height:14px;border-radius:50%;
      background:${color};border:2px solid white;
      box-shadow:0 1px 4px rgba(0,0,0,0.35);
    "></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
  });
}

function FitBounds({ points }: { points: [number, number][] }) {
  const map = useMap();
  if (points.length > 1) {
    map.fitBounds(L.latLngBounds(points), { padding: [40, 40] });
  }
  return null;
}

function formatTime(iso: string | null): string {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleTimeString("en-IE", {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

function CauseRow({ cause }: { cause: DisruptionCause }) {
  return (
    <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1.5, py: 0.5 }}>
      <Box
        sx={{
          color: CONFIDENCE_COLORS[cause.confidence],
          mt: 0.2,
          flexShrink: 0,
        }}
      >
        {CAUSE_ICONS[cause.causeType] ?? (
          <WarningAmberIcon sx={{ fontSize: 18 }} />
        )}
      </Box>
      <Typography sx={{ fontSize: "0.9rem", color: "#374151", flex: 1 }}>
        {cause.causeDescription}
      </Typography>
      <Chip
        label={cause.confidence}
        size="small"
        sx={{
          fontSize: "0.65rem",
          bgcolor: CONFIDENCE_COLORS[cause.confidence] + "18",
          color: CONFIDENCE_COLORS[cause.confidence],
          border: `1px solid ${CONFIDENCE_COLORS[cause.confidence]}44`,
          fontWeight: 700,
          flexShrink: 0,
        }}
      />
    </Box>
  );
}

function AlternativeRow({ alt }: { alt: DisruptionAlternative }) {
  const color = modeColor(alt.mode);
  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 1.5,
        py: 0.75,
        px: 1,
        borderRadius: 1.5,
        "&:hover": { bgcolor: "#F9FAFB" },
      }}
    >
      <Box
        sx={{
          color,
          flexShrink: 0,
          width: 32,
          height: 32,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          bgcolor: color + "18",
          borderRadius: "50%",
        }}
      >
        {modeIcon(alt.mode)}
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography
          sx={{
            fontSize: "0.9rem",
            fontWeight: 600,
            color: "#111827",
            whiteSpace: "nowrap",
            overflow: "hidden",
            textOverflow: "ellipsis",
          }}
        >
          {alt.stopName ?? alt.description}
        </Typography>
        <Typography sx={{ fontSize: "0.78rem", color: "#6B7280" }}>
          {alt.description}
        </Typography>
        {alt.availabilityCount != null && (
          <Chip
            label={`${alt.availabilityCount} bikes`}
            size="small"
            sx={{ fontSize: "0.65rem", mt: 0.25 }}
          />
        )}
      </Box>
      {alt.googleMapsWalkingUrl && (
        <Tooltip title="Get walking directions">
          <IconButton
            component={Link}
            href={alt.googleMapsWalkingUrl}
            target="_blank"
            rel="noopener noreferrer"
            size="small"
            sx={{ color: "#3B82F6", flexShrink: 0 }}
          >
            <DirectionsWalkIcon sx={{ fontSize: 20 }} />
          </IconButton>
        </Tooltip>
      )}
    </Box>
  );
}

export const PublicDisruptionPage = () => {
  const { id } = useParams<{ id: string }>();
  const numericId = id ? Number.parseInt(id, 10) : null;

  const {
    data: disruption,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["public", "disruption", numericId],
    queryFn: () => dashboardApi.getPublicDisruption(numericId!),
    enabled: numericId != null && !Number.isNaN(numericId),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });

  if (isLoading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "100vh",
        }}
      >
        <CircularProgress sx={{ color: "#EF4444" }} />
      </Box>
    );
  }

  if (error || !disruption) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "100vh",
          bgcolor: "#F9FAFB",
        }}
      >
        <Box sx={{ textAlign: "center" }}>
          <WarningAmberIcon sx={{ fontSize: 48, color: "#9CA3AF", mb: 2 }} />
          <Typography sx={{ fontSize: "1.1rem", color: "#6B7280" }}>
            Disruption not found or no longer active.
          </Typography>
        </Box>
      </Box>
    );
  }

  const severityColor = SEVERITY_COLORS[disruption.severity] ?? "#6B7280";
  const causes = disruption.causes ?? [];
  const alternatives = (disruption.alternatives ?? []).filter(
    (a) => a.lat != null && a.lon != null,
  );
  const hasMap = disruption.latitude != null && disruption.longitude != null;

  // All points for map bounds: disruption + all alternatives
  const mapPoints: [number, number][] = [];
  if (hasMap) mapPoints.push([disruption.latitude!, disruption.longitude!]);
  for (const a of alternatives) {
    if (a.lat != null && a.lon != null) mapPoints.push([a.lat, a.lon]);
  }

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "#F3F4F6" }}>
      {/* Severity header */}
      <Box
        sx={{
          bgcolor: severityColor,
          px: { xs: 2, md: 4 },
          py: 2,
          display: "flex",
          alignItems: "center",
          gap: 1.5,
        }}
      >
        <WarningAmberIcon sx={{ color: "#fff", fontSize: 26 }} />
        <Box sx={{ flex: 1 }}>
          <Typography
            sx={{
              color: "#fff",
              fontWeight: 800,
              fontSize: "1rem",
              letterSpacing: -0.2,
            }}
          >
            SERVICE DISRUPTION
          </Typography>
          <Typography
            sx={{ color: "rgba(255,255,255,0.85)", fontSize: "0.82rem" }}
          >
            {disruption.affectedArea}
            {disruption.detectedAt
              ? ` · Detected ${formatTime(disruption.detectedAt)}`
              : ""}
          </Typography>
        </Box>
        <Chip
          label={disruption.severity}
          sx={{
            bgcolor: "rgba(255,255,255,0.22)",
            color: "#fff",
            fontWeight: 700,
            fontSize: "0.72rem",
            border: "1px solid rgba(255,255,255,0.4)",
          }}
        />
      </Box>

      {/* Two-column layout: map left (or top on mobile), info right */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "1fr 380px" },
          minHeight: "calc(100vh - 72px)",
        }}
      >
        {/* MAP */}
        <Box sx={{ position: "relative", minHeight: { xs: 280, md: "100%" } }}>
          {hasMap ? (
            <MapContainer
              center={[disruption.latitude!, disruption.longitude!]}
              zoom={15}
              style={{ height: "100%", minHeight: 280, width: "100%" }}
              zoomControl={false}
            >
              <TileLayer
                attribution='&copy; <a href="https://osm.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              {mapPoints.length > 1 && <FitBounds points={mapPoints} />}

              {/* Disruption marker */}
              <Marker
                position={[disruption.latitude!, disruption.longitude!]}
                icon={makeDisruptionIcon(severityColor)}
              >
                <Popup>
                  <strong>{disruption.affectedArea}</strong>
                  <br />
                  {disruption.disruptionType} — {disruption.severity}
                </Popup>
              </Marker>

              {/* Alternative markers */}
              {alternatives.map((a) => (
                <Marker
                  key={a.id}
                  position={[a.lat!, a.lon!]}
                  icon={makeAltIcon(modeColor(a.mode))}
                >
                  <Popup>
                    <strong>
                      {{
                        bus: "Bus",
                        tram: "Luas",
                        rail: "Irish Rail",
                        bike: "DublinBikes",
                      }[a.mode?.toLowerCase() ?? ""] ?? a.mode}{" "}
                      · {a.stopName}
                    </strong>
                    {a.description && (
                      <>
                        <br />
                        {a.description}
                      </>
                    )}
                    {a.availabilityCount != null && (
                      <>
                        <br />
                        {a.availabilityCount} bikes available
                      </>
                    )}
                    {a.googleMapsWalkingUrl && (
                      <>
                        <br />
                        <a
                          href={a.googleMapsWalkingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          Walk here ↗
                        </a>
                      </>
                    )}
                  </Popup>
                </Marker>
              ))}
            </MapContainer>
          ) : (
            <Box
              sx={{
                height: "100%",
                minHeight: 280,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                bgcolor: "#E5E7EB",
              }}
            >
              <Typography sx={{ color: "#9CA3AF", fontSize: "0.9rem" }}>
                Location not available
              </Typography>
            </Box>
          )}
        </Box>

        {/* INFO PANEL */}
        <Box
          sx={{
            bgcolor: "#fff",
            overflowY: "auto",
            display: "flex",
            flexDirection: "column",
            boxShadow: { md: "-2px 0 12px rgba(0,0,0,0.06)" },
          }}
        >
          {/* Description */}
          <Box sx={{ px: 3, pt: 2.5, pb: 1.5 }}>
            <Typography
              sx={{
                fontSize: "1.2rem",
                fontWeight: 800,
                color: "#111827",
                lineHeight: 1.25,
                mb: 0.75,
              }}
            >
              {disruption.name ??
                TYPE_LABELS[disruption.disruptionType] ??
                disruption.disruptionType}
            </Typography>
            {disruption.description && (
              <Typography
                sx={{ fontSize: "0.9rem", color: "#374151", lineHeight: 1.55 }}
              >
                {disruption.description}
              </Typography>
            )}
          </Box>

          {/* Causes */}
          {causes.length > 0 && (
            <>
              <Divider sx={{ mx: 3 }} />
              <Box sx={{ px: 3, py: 1.5 }}>
                <Typography
                  sx={{
                    fontSize: "0.72rem",
                    fontWeight: 700,
                    color: "#9CA3AF",
                    textTransform: "uppercase",
                    letterSpacing: 0.8,
                    mb: 0.75,
                  }}
                >
                  Why is this happening?
                </Typography>
                <Stack spacing={0.25}>
                  {causes.map((c) => (
                    <CauseRow key={c.id} cause={c} />
                  ))}
                </Stack>
              </Box>
            </>
          )}

          {/* Alternatives grouped by mode */}
          {alternatives.length > 0 && (
            <>
              <Divider sx={{ mx: 3 }} />
              <Box sx={{ px: 2, py: 1.5, flex: 1 }}>
                <Typography
                  sx={{
                    fontSize: "0.72rem",
                    fontWeight: 700,
                    color: "#9CA3AF",
                    textTransform: "uppercase",
                    letterSpacing: 0.8,
                    mb: 0.5,
                    px: 1,
                  }}
                >
                  Nearby alternatives
                </Typography>
                {(() => {
                  const KNOWN_ORDER = ["bus", "tram", "rail", "bike"];
                  const modeLabels: Record<string, string> = {
                    bus: "Bus",
                    tram: "Luas",
                    rail: "Irish Rail",
                    bike: "DublinBikes",
                  };
                  const seenModes = [
                    ...new Set(
                      alternatives.map((a) => a.mode?.toLowerCase() ?? "other"),
                    ),
                  ].toSorted(
                    (a, b) =>
                      (KNOWN_ORDER.includes(a) ? KNOWN_ORDER.indexOf(a) : 99) -
                      (KNOWN_ORDER.includes(b) ? KNOWN_ORDER.indexOf(b) : 99),
                  );
                  return seenModes.map((mode) => {
                    const group = alternatives.filter(
                      (a) => (a.mode?.toLowerCase() ?? "other") === mode,
                    );
                    if (group.length === 0) return null;
                    const color = modeColor(mode);
                    return (
                      <Box key={mode} sx={{ mb: 1 }}>
                        <Box
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 0.75,
                            px: 1,
                            mb: 0.25,
                          }}
                        >
                          <Box sx={{ color, display: "flex" }}>
                            {modeIcon(mode)}
                          </Box>
                          <Typography
                            sx={{
                              fontSize: "0.72rem",
                              fontWeight: 700,
                              color,
                              textTransform: "uppercase",
                              letterSpacing: 0.6,
                            }}
                          >
                            {modeLabels[mode] ??
                              mode.charAt(0).toUpperCase() + mode.slice(1)}
                          </Typography>
                        </Box>
                        <Stack spacing={0}>
                          {group.map((a) => (
                            <AlternativeRow key={a.id} alt={a} />
                          ))}
                        </Stack>
                      </Box>
                    );
                  });
                })()}
              </Box>
            </>
          )}

          {alternatives.length === 0 && (
            <Box sx={{ px: 3, py: 2 }}>
              <Typography sx={{ fontSize: "0.88rem", color: "#6B7280" }}>
                Check real-time displays at the stop or use the TFI Live app for
                service updates.
              </Typography>
            </Box>
          )}

          {/* Footer */}
          <Box
            sx={{
              mx: 3,
              borderTop: "1px solid #E5E7EB",
              mt: "auto",
              py: 1.5,
              display: "flex",
              alignItems: "center",
              gap: 1,
            }}
          >
            <Box
              sx={{
                width: 7,
                height: 7,
                borderRadius: "50%",
                bgcolor: severityColor,
                flexShrink: 0,
                animation: "pulse 1.5s ease-in-out infinite",
                "@keyframes pulse": {
                  "0%, 100%": { opacity: 1 },
                  "50%": { opacity: 0.3 },
                },
              }}
            />
            <Typography sx={{ fontSize: "0.75rem", color: "#9CA3AF" }}>
              Actively managed · updates every minute
            </Typography>
          </Box>
        </Box>
      </Box>
    </Box>
  );
};
