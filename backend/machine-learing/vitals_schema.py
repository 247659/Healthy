from pydantic import BaseModel, Field
from datetime import datetime

class VitalsPayload(BaseModel):
    patientId: str
    timestamp: datetime
    heartRate: int
    systolicBp: int
    diastolicBp: int
    temperature: float
    spO2: int