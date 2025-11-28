import React from 'react';
import { DirectionsWalk } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { usePedestrianIndicators } from '@/hooks';

const PedestrianIndicator: React.FC = () => {
  return (
    <TransportIndicator
      mode="Pedestrian"
      icon={DirectionsWalk}
      useIndicatorHook={usePedestrianIndicators}
      color="success"
    />
  );
};

export default PedestrianIndicator;
