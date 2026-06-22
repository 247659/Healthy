import { apiClient } from './apiClient';

export const patientClient = {
    updatePatient: async (patientId: string, data: any) => {
        const response = await apiClient.put(`/patients/update/${patientId}`, data);
        return response.data;
    }
};