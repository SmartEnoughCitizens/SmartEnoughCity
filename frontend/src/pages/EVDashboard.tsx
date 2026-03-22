/**
 * EV Charging dashboard — full-viewport map with floating stats panel.
 * Displayed as a sub-tab of CarDashboard (visible via CarDashboard tab bar).
 *
 * Map caching: MapContainer is ALWAYS rendered (never conditionally unmounted).
 * Loading/empty states are overlaid on top so Leaflet is not re-initialised
 * whenever the parent CarDashboard switches visibility via CSS.
 */

import { useState, useMemo, useEffect } from "react";
import {
  Box,
  Chip,
  CircularProgress,
  Paper,
  Typography,
  IconButton,
  Divider,
} from "@mui/material";
import EvStationIcon from "@mui/icons-material/EvStation";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CloseIcon from "@mui/icons-material/Close";
import ElectricBoltIcon from "@mui/icons-material/ElectricBolt";
import { MapContainer, TileLayer, Marker, Popup, GeoJSON, useMap } from "react-leaflet";
import { useEvChargingStations, useEvChargingDemand, useEvAreasGeoJson } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import type { EvAreaDemand } from "@/types";
import type { PathOptions } from "leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// ─── Custom Electric Bolt Icon ────────────────────────────────────────────────

const electricBoltIcon = L.divIcon({
  className: "custom-ev-marker",
  html: `
    <div style="
      background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
      width: 24px;
      height: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(99, 102, 241, 0.4);
      border: 2px solid white;
      cursor: pointer;
      transition: transform 0.2s ease;
    ">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="white">
        <path d="M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z"/>
      </svg>
    </div>
  `,
  iconSize: [24, 24],
  iconAnchor: [12, 12],
  popupAnchor: [0, -12],
});

// ─── Map Controller for Zoom ──────────────────────────────────────────────────

const MapController = ({ center, zoom }: { center: [number, number] | null; zoom: number }) => {
  const map = useMap();

  useEffect(() => {
    if (center) {
      map.setView(center, zoom, { animate: true, duration: 0.5 });
    }
  }, [center, zoom, map]);

  return null;
};

// ─── Demand Overlay using GeoJSON Polygons ───────────────────────────────────

const DemandOverlay = ({
  geoJsonData,
  demandData,
  show,
  maxDemand,
  demandFilter,
}: {
  geoJsonData: any;
  demandData: Record<string, EvAreaDemand>;
  show: boolean;
  maxDemand: number;
  demandFilter: string[];
}) => {
  if (!show || !geoJsonData) return null;

  const getDemandColor = (demand: number, max: number): string => {
    const ratio = demand / max;
    if (ratio > 0.66) return "#fca5a5"; // High - pastel red
    if (ratio > 0.33) return "#fdba74"; // Medium - pastel orange
    return "#86efac"; // Low - pastel green
  };

  const getDemandBorderColor = (demand: number, max: number): string => {
    const ratio = demand / max;
    if (ratio > 0.66) return "#f87171"; // High - slightly darker pastel red
    if (ratio > 0.33) return "#fb923c"; // Medium - slightly darker pastel orange
    return "#4ade80"; // Low - slightly darker pastel green
  };

  const getDemandLevel = (demand: number, max: number): string => {
    const ratio = demand / max;
    if (ratio > 0.66) return "high";
    if (ratio > 0.33) return "medium";
    return "low";
  };

  const getFeatureStyle = (feature: any): PathOptions => {
    const chargingDemand = feature.properties?.charging_demand;

    if (!chargingDemand || chargingDemand === null) {
      // Areas without demand data - completely transparent
      return {
        color: "transparent",
        weight: 0,
        fillColor: "transparent",
        fillOpacity: 0,
      };
    }

    const demandLevel = getDemandLevel(chargingDemand, maxDemand);

    // Hide if not in filter
    if (!demandFilter.includes(demandLevel)) {
      return {
        color: "transparent",
        weight: 0,
        fillColor: "transparent",
        fillOpacity: 0,
      };
    }

    // Make overlay clearly visible with proper colors
    return {
      color: getDemandBorderColor(chargingDemand, maxDemand),
      weight: 2,
      fillColor: getDemandColor(chargingDemand, maxDemand),
      fillOpacity: 0.5,
      opacity: 1,
    };
  };

  const onEachFeature = (feature: any, layer: any) => {
    const areaName = feature.properties?.display_name || "Unknown Area";
    const chargingDemand = feature.properties?.charging_demand;
    const registeredEv = feature.properties?.registered_ev;

    if (chargingDemand && chargingDemand !== null) {
      // Show popup on hover
      layer.bindPopup(`
        <strong>${areaName}</strong><br />
        Registered EVs: ${registeredEv || 'N/A'}<br />
        Charging Demand: ${chargingDemand.toFixed(1)}
      `);

      // Highlight more on hover
      layer.on({
        mouseover: (e: any) => {
          const layer = e.target;
          layer.setStyle({
            fillOpacity: 0.7,
            weight: 3,
            color: getDemandBorderColor(chargingDemand, maxDemand),
            opacity: 1,
          });
          layer.openPopup();
        },
        mouseout: (e: any) => {
          const layer = e.target;
          layer.setStyle({
            fillOpacity: 0.5,
            weight: 2,
            color: getDemandBorderColor(chargingDemand, maxDemand),
            opacity: 1,
          });
        },
      });
    } else {
      // No demand data - no popup
    }
  };

  // Debug: log if data exists
  console.log("DemandOverlay rendering:", {
    show,
    hasData: !!geoJsonData,
    featuresCount: geoJsonData?.features?.length || 0,
    maxDemand,
    demandDataKeys: Object.keys(demandData).length
  });

  return <GeoJSON key={`geojson-${show}`} data={geoJsonData} style={getFeatureStyle} onEachFeature={onEachFeature} />;
};


// ─── Search Bar with Autocomplete ────────────────────────────────────────────

const SearchBar = ({
  searchQuery,
  onSearchChange,
  searchResults,
  onResultClick,
  totalStations,
  highPriorityAreas,
  isLoading,
  demandFilter,
  onDemandFilterChange,
}: {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  searchResults: { type: "station" | "area"; name: string; data: any }[];
  onResultClick: (result: any) => void;
  totalStations: number;
  highPriorityAreas: string[];
  isLoading: boolean;
  demandFilter: string[];
  onDemandFilterChange: (filters: string[]) => void;
}) => {
  const [isFocused, setIsFocused] = useState(false);
  const showResults = isFocused && searchQuery.trim().length > 0 && searchResults.length > 0;

  const toggleFilter = (filter: string) => {
    if (demandFilter.includes(filter)) {
      onDemandFilterChange(demandFilter.filter(f => f !== filter));
    } else {
      onDemandFilterChange([...demandFilter, filter]);
    }
  };

  return (
    <Box sx={{ position: "absolute", top: 16, left: 16, zIndex: 1000, width: 400 }}>
      <Paper
        elevation={0}
        sx={{
          borderRadius: 2,
          px: 2,
          py: 1.5,
          backdropFilter: "blur(10px)",
          bgcolor: (t) =>
            t.palette.mode === "dark"
              ? "rgba(30, 30, 30, 0.95)"
              : "rgba(255, 255, 255, 0.95)",
          border: (t) =>
            isFocused
              ? `2px solid ${t.palette.primary.main}`
              : `1px solid ${t.palette.mode === "dark" ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"}`,
          transition: "all 0.2s ease",
        }}
      >
        {/* Search Input */}
        <Box
          component="input"
          placeholder="Search stations or areas..."
          value={searchQuery}
          onChange={(e: any) => onSearchChange(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setTimeout(() => setIsFocused(false), 200)}
          sx={{
            width: "100%",
            border: "none",
            outline: "none",
            bgcolor: "transparent",
            fontSize: "0.95rem",
            fontFamily: "inherit",
            color: "text.primary",
            "&::placeholder": {
              color: "text.secondary",
              opacity: 0.6,
            },
          }}
        />

        {/* Stats & Filters */}
        {!isLoading && (
          <>
            <Box sx={{ display: "flex", gap: 1, mt: 1.5 }}>
              <Chip
                icon={<EvStationIcon sx={{ fontSize: "14px !important" }} />}
                label={`${totalStations} Stations`}
                color="primary"
                size="small"
                sx={{ fontSize: "0.75rem", height: 24 }}
              />
              <Chip
                icon={<WarningAmberIcon sx={{ fontSize: "14px !important" }} />}
                label={`${highPriorityAreas.length} High Demand`}
                color="warning"
                size="small"
                sx={{ fontSize: "0.75rem", height: 24 }}
              />
            </Box>

            {/* Demand Filters */}
            <Box sx={{ display: "flex", gap: 0.75, mt: 1, flexWrap: "wrap" }}>
              <Chip
                label="High Demand"
                size="small"
                clickable
                onClick={() => toggleFilter("high")}
                sx={{
                  fontSize: "0.7rem",
                  height: 22,
                  bgcolor: demandFilter.includes("high") ? "#fca5a5" : "transparent",
                  border: `1px solid #f87171`,
                  color: demandFilter.includes("high") ? "#000" : "text.secondary",
                  "&:hover": {
                    bgcolor: "#fca5a5",
                  },
                }}
              />
              <Chip
                label="Medium Demand"
                size="small"
                clickable
                onClick={() => toggleFilter("medium")}
                sx={{
                  fontSize: "0.7rem",
                  height: 22,
                  bgcolor: demandFilter.includes("medium") ? "#fdba74" : "transparent",
                  border: `1px solid #fb923c`,
                  color: demandFilter.includes("medium") ? "#000" : "text.secondary",
                  "&:hover": {
                    bgcolor: "#fdba74",
                  },
                }}
              />
              <Chip
                label="Low Demand"
                size="small"
                clickable
                onClick={() => toggleFilter("low")}
                sx={{
                  fontSize: "0.7rem",
                  height: 22,
                  bgcolor: demandFilter.includes("low") ? "#86efac" : "transparent",
                  border: `1px solid #4ade80`,
                  color: demandFilter.includes("low") ? "#000" : "text.secondary",
                  "&:hover": {
                    bgcolor: "#86efac",
                  },
                }}
              />
            </Box>
          </>
        )}
      </Paper>

      {/* Search Results Dropdown */}
      {showResults && (
        <Paper
          elevation={3}
          sx={{
            mt: 1,
            borderRadius: 2,
            maxHeight: 300,
            overflow: "auto",
            backdropFilter: "blur(10px)",
            bgcolor: (t) =>
              t.palette.mode === "dark"
                ? "rgba(30, 30, 30, 0.95)"
                : "rgba(255, 255, 255, 0.95)",
          }}
        >
          {searchResults.map((result, idx) => (
            <Box
              key={idx}
              onClick={() => onResultClick(result)}
              sx={{
                px: 2,
                py: 1.5,
                cursor: "pointer",
                borderBottom: (t) => `1px solid ${t.palette.divider}`,
                "&:hover": {
                  bgcolor: (t) =>
                    t.palette.mode === "dark"
                      ? "rgba(255,255,255,0.05)"
                      : "rgba(0,0,0,0.03)",
                },
                "&:last-child": {
                  borderBottom: "none",
                },
              }}
            >
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <Chip
                  label={result.type}
                  size="small"
                  color={result.type === "station" ? "primary" : "secondary"}
                  sx={{ fontSize: "0.7rem", height: 20, textTransform: "capitalize" }}
                />
                <Box
                  component="span"
                  sx={{
                    fontSize: "0.875rem",
                    fontWeight: 500,
                    color: "text.primary",
                  }}
                >
                  {result.name}
                </Box>
              </Box>
            </Box>
          ))}
        </Paper>
      )}
    </Box>
  );
};

// ─── Side Panel for Stations ──────────────────────────────────────────────────

const SidePanel = ({
  stations,
  selectedArea,
  onClose,
}: {
  stations: any[];
  selectedArea: string | null;
  onClose: () => void;
}) => (
  <Paper
    elevation={0}
    sx={{
      position: "absolute",
      top: 16,
      right: 16,
      bottom: 16,
      width: 350,
      zIndex: 1000,
      borderRadius: 2,
      display: "flex",
      flexDirection: "column",
      backdropFilter: "blur(10px)",
      bgcolor: (t) =>
        t.palette.mode === "dark"
          ? "rgba(30, 30, 30, 0.95)"
          : "rgba(255, 255, 255, 0.95)",
    }}
  >
    {/* Header */}
    <Box
      sx={{
        p: 2,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        borderBottom: (t) => `1px solid ${t.palette.divider}`,
      }}
    >
      <Box>
        <Typography variant="h6" fontWeight={700}>
          {selectedArea || "Stations"}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {stations.length} {stations.length === 1 ? "station" : "stations"}
        </Typography>
      </Box>
      <IconButton size="small" onClick={onClose}>
        <CloseIcon fontSize="small" />
      </IconButton>
    </Box>

    {/* Stations List */}
    <Box sx={{ flex: 1, overflow: "auto", p: 2 }}>
      {stations.map((station, idx) => (
        <Box
          key={idx}
          sx={{
            mb: 2,
            p: 1.5,
            borderRadius: 2,
            bgcolor: (t) =>
              t.palette.mode === "dark"
                ? "rgba(255,255,255,0.03)"
                : "rgba(0,0,0,0.025)",
            "&:last-child": { mb: 0 },
          }}
        >
          <Typography variant="body2" fontWeight={600} noWrap>
            {station.address}
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block">
            {station.county}
          </Typography>
          <Box sx={{ display: "flex", gap: 0.75, mt: 1, flexWrap: "wrap" }}>
            <Chip
              size="small"
              icon={<ElectricBoltIcon sx={{ fontSize: "12px !important" }} />}
              label={`${station.charger_count} charger${station.charger_count !== 1 ? "s" : ""}`}
              sx={{ fontSize: "0.7rem", height: 20 }}
            />
            <Chip
              size="small"
              label={station.open_hours}
              variant="outlined"
              sx={{ fontSize: "0.7rem", height: 20 }}
            />
          </Box>
        </Box>
      ))}
    </Box>
  </Paper>
);

// ─── Legend ───────────────────────────────────────────────────────────────────

const Legend = () => (
  <Paper
    elevation={0}
    sx={{
      position: "absolute",
      bottom: 16,
      left: 16,
      zIndex: 1000,
      p: 1.5,
      borderRadius: 2,
      backdropFilter: "blur(10px)",
      bgcolor: (t) =>
        t.palette.mode === "dark"
          ? "rgba(30, 30, 30, 0.95)"
          : "rgba(255, 255, 255, 0.95)",
    }}
  >
    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" sx={{ mb: 1 }}>
      LEGEND
    </Typography>
    <Box sx={{ display: "flex", flexDirection: "column", gap: 0.75 }}>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <Box
          sx={{
            width: 16,
            height: 16,
            borderRadius: "50%",
            bgcolor: "#6366f1",
            border: "2px solid #fff",
          }}
        />
        <Typography variant="caption">Charging Station</Typography>
      </Box>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <Box
          sx={{
            width: 16,
            height: 16,
            borderRadius: 1,
            bgcolor: "#fca5a5",
            border: "1px solid #f87171",
          }}
        />
        <Typography variant="caption">High Demand Area</Typography>
      </Box>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <Box
          sx={{
            width: 16,
            height: 16,
            borderRadius: 1,
            bgcolor: "#fdba74",
            border: "1px solid #fb923c",
          }}
        />
        <Typography variant="caption">Medium Demand Area</Typography>
      </Box>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <Box
          sx={{
            width: 16,
            height: 16,
            borderRadius: 1,
            bgcolor: "#86efac",
            border: "1px solid #4ade80",
          }}
        />
        <Typography variant="caption">Low Demand Area</Typography>
      </Box>
    </Box>
  </Paper>
);

// ─── Main component ───────────────────────────────────────────────────────────

export const EVDashboard = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedArea, setSelectedArea] = useState<string | null>(null);
  const [selectedStations, setSelectedStations] = useState<any[]>([]);
  const [mapCenter, setMapCenter] = useState<[number, number] | null>(null);
  const [mapZoom, setMapZoom] = useState(12);
  const [demandFilter, setDemandFilter] = useState<string[]>(["high", "medium", "low"]);
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: stationsData, isLoading: stationsLoading } =
    useEvChargingStations();
  const { data: demandData, isLoading: demandLoading } = useEvChargingDemand();
  const { data: geoJsonData, isLoading: geoJsonLoading } = useEvAreasGeoJson();

  const isLoading = stationsLoading || demandLoading || geoJsonLoading;
  const allStations = stationsData?.stations ?? [];
  const totalStations = stationsData?.total_stations ?? 0;
  const highPriorityAreas = demandData?.high_priority_areas ?? [];
  const demandAreas = demandData?.areas ?? [];

  // Get unique areas from GeoJSON
  const uniqueAreas = useMemo(() => {
    if (!geoJsonData || !geoJsonData.features) return [];
    const areaNames = geoJsonData.features
      .map((f: any) => f.properties?.display_name)
      .filter((name: string) => name && name !== null);
    return Array.from(new Set(areaNames)).sort();
  }, [geoJsonData]);

  // Create search results
  const searchResults = useMemo(() => {
    if (!searchQuery.trim()) return [];
    const query = searchQuery.toLowerCase();
    const results: { type: "station" | "area"; name: string; data: any }[] = [];

    // Add matching areas from GeoJSON
    uniqueAreas.forEach((area) => {
      if (area.toLowerCase().includes(query)) {
        results.push({ type: "area", name: area, data: area });
      }
    });

    // Add matching stations
    allStations.forEach((station) => {
      if (station.address.toLowerCase().includes(query)) {
        results.push({ type: "station", name: station.address, data: station });
      }
    });

    return results.slice(0, 10); // Limit to 10 results
  }, [allStations, uniqueAreas, searchQuery]);

  // Handle search result click
  const handleResultClick = (result: any) => {
    setSearchQuery("");

    if (result.type === "area") {
      // Find the GeoJSON feature for this area
      const areaFeature = geoJsonData?.features.find(
        (f: any) => f.properties?.display_name === result.data
      );

      if (areaFeature && areaFeature.geometry.type === "Polygon") {
        // Calculate center of polygon (simple average of all coordinates)
        const coords = areaFeature.geometry.coordinates[0];
        const avgLng = coords.reduce((sum: number, c: number[]) => sum + c[0], 0) / coords.length;
        const avgLat = coords.reduce((sum: number, c: number[]) => sum + c[1], 0) / coords.length;

        // Find stations near this area (within a radius)
        const nearbyStations = allStations.filter((station) => {
          const latDiff = Math.abs(station.latitude - avgLat);
          const lngDiff = Math.abs(station.longitude - avgLng);
          return latDiff < 0.05 && lngDiff < 0.05; // ~5km radius
        });

        setSelectedArea(result.data);
        setSelectedStations(nearbyStations.length > 0 ? nearbyStations : allStations.slice(0, 5));
        setMapCenter([avgLat, avgLng]);
        setMapZoom(14);
      }
    } else {
      // Single station selected
      setSelectedArea(result.data.address);
      setSelectedStations([result.data]);
      setMapCenter([result.data.latitude, result.data.longitude]);
      setMapZoom(15);
    }
  };

  // Filter stations based on search query for display on map
  const filteredStations = useMemo(() => {
    if (!searchQuery.trim()) return allStations;
    const query = searchQuery.toLowerCase();
    return allStations.filter(
      (station) =>
        station.address.toLowerCase().includes(query) ||
        station.county.toLowerCase().includes(query)
    );
  }, [allStations, searchQuery]);

  // Create a map of area name to demand data for quick lookup
  const demandDataMap = useMemo(() => {
    const map: Record<string, EvAreaDemand> = {};
    demandAreas.forEach((area) => {
      map[area.area] = area;
    });
    return map;
  }, [demandAreas]);

  const maxDemand = demandAreas.length
    ? Math.max(...demandAreas.map((area) => area.charging_demand))
    : 1;

  const dublinCenter: [number, number] = [53.3498, -6.2603];

  const tileUrl =
    theme === "dark"
      ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
      : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

  const tileAttribution =
    theme === "dark"
      ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
      : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/*
       * MapContainer is ALWAYS rendered — never conditionally unmounted.
       * This prevents Leaflet re-initialisation on every CarDashboard tab switch.
       * Loading / empty states are layered on top via absolute positioning.
       */}
      <MapContainer
        center={dublinCenter}
        zoom={12}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
      >
        <TileLayer attribution={tileAttribution} url={tileUrl} />

        {/* Map controller for zooming */}
        <MapController center={mapCenter} zoom={mapZoom} />

        {/* Demand overlay with filter */}
        <DemandOverlay
          geoJsonData={geoJsonData}
          demandData={demandDataMap}
          show={true}
          maxDemand={maxDemand}
          demandFilter={demandFilter}
        />

        {/* Station markers - render with custom electric bolt icon */}
        {allStations.map((station, idx) => (
          <Marker
            key={`${station.address}-${idx}`}
            position={[station.latitude, station.longitude]}
            icon={electricBoltIcon}
            eventHandlers={{
              click: () => {
                setSelectedArea(station.county);
                setSelectedStations([station]);
                setMapCenter([station.latitude, station.longitude]);
                setMapZoom(15);
              },
            }}
          >
            <Popup>
              <strong>{station.address}</strong>
              <br />
              {station.county}
              <br />
              Chargers: {station.charger_count}
              <br />
              Hours: {station.open_hours}
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      {/* Map loading overlay — sits above map, does not unmount it */}
      {isLoading && (
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            zIndex: 500,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            backdropFilter: "blur(4px)",
            bgcolor: (t) =>
              t.palette.mode === "dark"
                ? "rgba(15,23,42,0.4)"
                : "rgba(255,255,255,0.4)",
          }}
        >
          <CircularProgress />
        </Box>
      )}

      {/* Search bar with autocomplete */}
      <SearchBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        searchResults={searchResults}
        onResultClick={handleResultClick}
        totalStations={totalStations}
        highPriorityAreas={highPriorityAreas}
        isLoading={isLoading}
        demandFilter={demandFilter}
        onDemandFilterChange={setDemandFilter}
      />

      {/* Side panel for selected stations */}
      {selectedStations.length > 0 && (
        <SidePanel
          stations={selectedStations}
          selectedArea={selectedArea}
          onClose={() => {
            setSelectedStations([]);
            setSelectedArea(null);
            setMapCenter(null);
            setMapZoom(12);
          }}
        />
      )}

      {/* Legend */}
      <Legend />
    </Box>
  );
};
