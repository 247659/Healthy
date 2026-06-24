import { apiClient } from './apiClient';

export const vitalsClient = {
    getVitalsHistory: async (patientId: string, limit: number = 100) => {
        const response = await apiClient.get(`/vital-signs/patient/${patientId}?limit=${limit}`);
        return response.data;
    },

    getAlerts: async (patientId: string) => {
        const response = await apiClient.get(`/notifications/${patientId}`);
        return response.data;
    }
};