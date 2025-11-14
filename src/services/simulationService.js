import api from './api';
import { API_ENDPOINTS } from '../constants';

export const simulationService = {
  // Get all simulations
  getAllSimulations: async () => {
    const response = await api.get(API_ENDPOINTS.SIMULATIONS);
    return response.data;
  },

  // Get simulation by ID
  getSimulationById: async (id) => {
    const response = await api.get(API_ENDPOINTS.SIMULATION_BY_ID(id));
    return response.data;
  },

  // Run new simulation
  runSimulation: async (simulationData) => {
    const response = await api.post(API_ENDPOINTS.RUN_SIMULATION, simulationData);
    return response.data;
  },

  // Delete simulation
  deleteSimulation: async (id) => {
    const response = await api.delete(API_ENDPOINTS.SIMULATION_BY_ID(id));
    return response.data;
  },
};

export default simulationService;
