import React, { ReactNode } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { LatLngExpression, PathOptions } from 'leaflet';
import { MAP_CONFIG } from '../../constants';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default marker icon
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

interface MapMarker {
  position: LatLngExpression;
  popup?: ReactNode;
}

interface MapCircle {
  center: LatLngExpression;
  radius: number;
  color?: string;
  fillColor?: string;
  popup?: ReactNode;
}

interface CityMapProps {
  title?: string;
  markers?: MapMarker[];
  circles?: MapCircle[];
  center?: LatLngExpression;
  zoom?: number;
  height?: number;
}

const CityMap: React.FC<CityMapProps> = ({
  title,
  markers = [],
  circles = [],
  center,
  zoom,
  height = 400
}) => {
  const mapCenter = center || MAP_CONFIG.center;
  const mapZoom = zoom || MAP_CONFIG.zoom;

  return (
    <Card>
      <CardContent>
        {title && (
          <Typography variant="h6" component="div" sx={{ mb: 2 }}>
            {title}
          </Typography>
        )}
        <Box sx={{ height, borderRadius: 1, overflow: 'hidden' }}>
          <MapContainer
            center={mapCenter}
            zoom={mapZoom}
            style={{ height: '100%', width: '100%' }}
            scrollWheelZoom={false}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {markers.map((marker, index) => (
              <Marker key={index} position={marker.position}>
                {marker.popup && <Popup>{marker.popup}</Popup>}
              </Marker>
            ))}
            {circles.map((circle, index) => (
              <Circle
                key={index}
                center={circle.center}
                radius={circle.radius}
                pathOptions={{ color: circle.color || 'blue', fillColor: circle.fillColor || 'blue', fillOpacity: 0.3 }}
              >
                {circle.popup && <Popup>{circle.popup}</Popup>}
              </Circle>
            ))}
          </MapContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default CityMap;
