import {api} from "./api.ts";
import type {Patient} from "../types/patient.ts";

export const gatewayService = {

    getAssignedPatients: async (doctorId: string): Promise<Patient[]> => {
        const response = await api.get<Patient[]>(
            `/gateway/dashboard/staff/${doctorId}/patients/assigned`
        );
        return response.data;
    },

    getUnassignedPatients: async (doctorId: string): Promise<Patient[]> => {
        const response = await api.get<Patient[]>(
            `/gateway/dashboard/staff/${doctorId}/patients/unassigned`
        );
        return response.data;
    }
}
