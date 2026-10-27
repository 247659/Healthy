from datetime import datetime
from typing import Optional
from pydantic import BaseModel


class BloodPressure(BaseModel):
    systolic: int
    diastolic: int


class Measurements(BaseModel):
    heartRate: int
    bloodPressure: BloodPressure
    temperature: float
    spO2: int


class VitalsPayload(BaseModel):
    patientId: str
    timestamp: datetime
    measurements: Measurements
    isAnomaly: Optional[bool] = False

    @property
    def heart_rate(self) -> int:
        return self.measurements.heartRate

    @property
    def systolic_bp(self) -> int:
        return self.measurements.bloodPressure.systolic

    @property
    def diastolic_bp(self) -> int:
        return self.measurements.bloodPressure.diastolic

    @property
    def temperature(self) -> float:
        return self.measurements.temperature

    @property
    def spO2(self) -> int:
        return self.measurements.spO2
