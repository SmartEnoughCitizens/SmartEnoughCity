import React from 'react';
import { Tram } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { indicatorService } from '../../services/indicatorService';

const TramIndicator = () => {
  return (
    <TransportIndicator
      mode="Tram"
      icon={Tram}
      service={indicatorService.getTramIndicators}
      color="info"
    />
  );
};

export default TramIndicator;
