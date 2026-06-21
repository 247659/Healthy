import {api} from "./api.ts";
import type {MedicalStaff} from "../types/medicalStaff.ts";

export const medicalStaffService = {
    getMedicalStaffInformationById: async (id: string): Promise<MedicalStaff> => {
        const response = await api.get<MedicalStaff>(
            `/staff/${id}`
        );
        return response.data;
    },

    assignPatient: async (doctorId: string, patientId: string) => {
        await api.post(
            `/staff/${doctorId}/assign/${patientId}`
        );
    },

    setupProfile: async (data: MedicalStaff): Promise<MedicalStaff> => {
        const response = await api.post<MedicalStaff>(
            `/staff`,
            data,
        )
        return response.data
    },

    editProfile: async (staffId: string, data: MedicalStaff): Promise<MedicalStaff> => {
        const response = await api.put<MedicalStaff>(
            `/staff/${staffId}`,
            data
        )
        return response.data
    },

    unassignPatient: async (doctorId: string, patientId: string) => {
        await api.delete(`/staff/${doctorId}/unassign/${patientId}`);
    },
}
