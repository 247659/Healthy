export interface Notification {
    alertId: string;
    patientId: string;
    riskScore: number;
    message: string;
    timestamp: string;
    isRead: boolean;
}