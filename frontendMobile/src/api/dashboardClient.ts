import { apiClient } from './apiClient';

export const dashboardClient = {
    getVitals: async (patientId: string, startDate: string, endDate: string) => {
        const response = await apiClient.get(`/vital-signs/patient/${patientId}?from=${startDate}&to=${endDate}`);
        return response.data;
    },

    getAssignedDoctors: async (patientId: string) => {
        const response = await apiClient.get(`/staff/patients/${patientId}`);
        return response.data;
    },

    getAlerts: async (patientId: string) => {
        const response = await apiClient.get(`/notifications/${patientId}`);
        return response.data;
    },

    getAllDoctors: async () => {
        const response = await apiClient.get(`/staff/essential`);
        return response.data;
    },

    assignDoctor: async (doctorId: string, patientId: string) => {
        const response = await apiClient.post(`/staff/${doctorId}/assign/${patientId}`);
        return response.data;
    },

    unassignDoctor: async (doctorId: string, patientId: string) => {
        const response = await apiClient.delete(`/staff/${doctorId}/unassign/${patientId}`);
        return response.data;
    }
};