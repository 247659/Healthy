from datetime import datetime

from schemas.vitals_schema import VitalsPayload
from repositories.vitals_repository import VitalsRepository


class VitalsService:
    def __init__(self, repository: VitalsRepository):
        self.repository = repository

    async def process_vitals(self, payload: VitalsPayload):
        await self.repository.save_measurement(payload)

        return {
            "status": "success",
            "message": "Pomiary zapisane w InfluxDB",
            "patientId": payload.patientId
        }

    async def get_vitals_measure_history(self, patient_id: str, start_time: datetime, end_time: datetime):
        records = await self.repository.get_measurements(patient_id, start_time, end_time)

        return {
            "patientId": patient_id,
            "measurementsCount": len(records),
            "history": records
        }