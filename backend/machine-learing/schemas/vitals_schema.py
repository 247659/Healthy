from pydantic import (BaseModel)
from datetime import datetime

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
