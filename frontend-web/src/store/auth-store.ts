import type {MedicalStaff} from "../types/medicalStaff.ts";
import {create} from "zustand";
import {persist} from "zustand/middleware";

interface AuthState {
    user: MedicalStaff | null;
    setUser: (user: MedicalStaff) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            user: null,

            setUser: (user) => set({ user }),

            logout: () => set({ user: null }),
        }),
        { name: 'healthy-auth' }
    )
);