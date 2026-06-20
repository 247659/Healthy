import type {Notification} from "../types/notification.ts";
import {api} from "./api.ts";

export const notificationService = {

    getNewNotifications: async (patientId : string): Promise<Notification[]> => {
        const response = await api.get<Notification[]>(
            `/notifications/${patientId}`
        );
        return response.data;
    }
}