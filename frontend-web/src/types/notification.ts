export interface Notification {
    alertId: string;
    patientId: string;
    riskScore: number;
    severity: string;
    message: string;
    details: string[];
    timestamp: string;
    isRead: boolean;
}