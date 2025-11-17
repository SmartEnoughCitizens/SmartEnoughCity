import React from 'react';
import { DirectionsBus } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { useBusIndicators } from '@/hooks';

const BusIndicator: React.FC = () => {
  return (
    <TransportIndicator
      mode="Bus"
      icon={DirectionsBus}
      useIndicatorHook={useBusIndicators}
      color="secondary"
    />
  );
};

export default BusIndicator;
