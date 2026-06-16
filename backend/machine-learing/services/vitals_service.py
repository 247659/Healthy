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