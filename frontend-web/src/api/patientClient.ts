import type {Patient} from "../types/patient.ts";
import {api} from "./api.ts";

export const patientService = {
    getAllPatients: async (): Promise<Patient[]> => {
        const response = await api.get<Patient[]>(
            `/patients/allPatients`
        )
        return response.data
    },


}