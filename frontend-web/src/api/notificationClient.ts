import type {Notification} from "../types/notification.ts";
import {api} from "./api.ts";

export const notificationService = {

    getNewNotifications: async (patientId: string, userId: string): Promise<Notification[]> => {
        const response = await api.get<Notification[]>(
            `/notifications/${patientId}`,
            {
                params: {
                    userId
                }
            }
        );
        return response.data;
    },

    getAllNotifications: async (patientId: string, userId: string): Promise<Notification[]> => {
        const response = await api.get<Notification[]>(
            `/notifications/all/${patientId}`,
            {
                params: {
                    userId
                }
            }
        );
        return response.data;
    },

    markAsRead: async (ids: string[], userId: string): Promise<void> => {
        await api.put<void>(
            `/notifications/mark-as-read`,
            {
                ids
            },
            {
                params: {
                    userId
                }
            }
        )
    }
}