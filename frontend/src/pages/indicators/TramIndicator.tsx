import React from 'react';
import { Tram } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { useTramIndicators } from '@/hooks';

const TramIndicator: React.FC = () => {
  return (
    <TransportIndicator
      mode="Tram"
      icon={Tram}
      useIndicatorHook={useTramIndicators}
      color="info"
    />
  );
};

export default TramIndicator;
