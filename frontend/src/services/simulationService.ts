import api from './api';
import { API_ENDPOINTS } from '../constants';
import { Simulation, CreateSimulationRequest } from '../types';

export const simulationService = {
  // Get all simulations
  getAllSimulations: async (): Promise<Simulation[]> => {
    const response = await api.get<Simulation[]>(API_ENDPOINTS.SIMULATIONS);
    return response.data;
  },

  // Get simulation by ID
  getSimulationById: async (id: string): Promise<Simulation> => {
    const response = await api.get<Simulation>(API_ENDPOINTS.SIMULATION_BY_ID(id));
    return response.data;
  },

  // Run new simulation
  runSimulation: async (simulationData: CreateSimulationRequest): Promise<Simulation> => {
    const response = await api.post<Simulation>(API_ENDPOINTS.RUN_SIMULATION, simulationData);
    return response.data;
  },

  // Delete simulation
  deleteSimulation: async (id: string): Promise<void> => {
    const response = await api.delete(API_ENDPOINTS.SIMULATION_BY_ID(id));
    return response.data;
  },
};

export default simulationService;
