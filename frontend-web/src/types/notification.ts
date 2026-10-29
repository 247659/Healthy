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

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}