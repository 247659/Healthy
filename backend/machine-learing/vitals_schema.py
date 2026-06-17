from pydantic import BaseModel
from datetime import datetime
from typing import Optional

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
    def heartRate(self) -> int:
        return self.measurements.heartRate

    @property
    def systolicBp(self) -> int:
        return self.measurements.bloodPressure.systolic

    @property
    def diastolicBp(self) -> int:
        return self.measurements.bloodPressure.diastolic

    @property
    def temperature(self) -> float:
        return self.measurements.temperature

    @property
    def spO2(self) -> int:
        return self.measurements.spO2