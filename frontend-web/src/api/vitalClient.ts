import type {PatientMeasurements} from "../types/vitals.ts";
import {api} from "./api.ts";

export const vitalService = {

    getHistory: async (patientId: string): Promise<PatientMeasurements[]> => {
        const response = await api.get<PatientMeasurements[]>(
            `/vital-signs/patient/${patientId}?`
        )
        return response.data
    },

    getHistoryByTime: async (patientId: string, time: string): Promise<PatientMeasurements[]> => {
        const response = await api.get<PatientMeasurements[]>(
            `/vital-signs/patient/${patientId}?from=${time}`
        )
        return response.data
    },
}