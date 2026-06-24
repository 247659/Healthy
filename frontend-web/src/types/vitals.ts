export interface PatientMeasurements {
    patientId: string;
    timestamp: string; // ISO-8601, np. "2026-06-20T12:34:56Z"
    measurements: Measurements;
}

export interface Measurements {
    heartRate: number;
    bloodPressure: BloodPressure;
    temperature: number;
    spO2: number;
}

export interface BloodPressure {
    systolic: number;
    diastolic: number;
}

export interface AlertObject {
    alertId: string;
    patientId: string;
    riskScore: number;
    severity: string;
    message: string;
    details: string[]; // List<String> w TS to string[]
    timestamp: string; // W JSON/TS daty zazwyczaj przechowujemy jako string (ISO)
    isRead: boolean;
}