import {create} from "zustand";
import type {Patient} from "../types/patient.ts";
import {persist} from "zustand/middleware";

interface PatientStore {
    assignedPatients: Patient[];
    unassignedPatients: Patient[];
    setAssignedPatients: (patients: Patient[]) => void;
    setUnassignedPatients: (patients: Patient[]) => void;
    addAssignedPatient: (patient: Patient) => void;
    addUnassignedPatient: (patient: Patient) => void;
    deleteAssignedPatient: (patient: Patient) => void;
    deleteUnassignedPatient: (patient: Patient) => void;
}

export const usePatientStore = create<PatientStore>()(
    persist((set) => ({
            assignedPatients: [],
            unassignedPatients: [],

            setAssignedPatients: (patients: Patient[]) => set({assignedPatients: patients}),
            setUnassignedPatients: (patients: Patient[]) => set({unassignedPatients: patients}),

            addAssignedPatient: (patient: Patient) => set((prev) => ({
                assignedPatients: [...prev.assignedPatients, patient],
            })),
            addUnassignedPatient: (patient: Patient) => set((prev) => ({
                unassignedPatients: [...prev.unassignedPatients, patient],
            })),

            deleteAssignedPatient: (patient: Patient) => set((prev) => ({
                assignedPatients: prev.assignedPatients.filter(t => t.id !== patient.id)
            })),
            deleteUnassignedPatient: (patient: Patient) => set((prev) => ({
                unassignedPatients: prev.unassignedPatients.filter(t => t.id !== patient.id)
            })),
        }),
        {name: 'healthy-patients'})
)