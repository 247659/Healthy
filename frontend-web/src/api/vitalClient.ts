import type {AlertObject, PatientMeasurements} from "../types/vitals.ts";
import {api} from "./api.ts";

export const vitalService = {

    getHistory: async (patientId: string): Promise<PatientMeasurements[]> => {
        const response = await api.get<PatientMeasurements[]>(
            `/vital-signs/patient/${patientId}?`
        )
        return response.data
    },

    getAlerts: async (patientId: string): Promise<AlertObject[]> => {
        const response = await api.get<AlertObject[]>(
            `/notifications/${patientId}`
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