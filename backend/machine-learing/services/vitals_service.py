from datetime import datetime

from ai.anomaly_detector import VitalsAnomalyDetector
from schemas.vitals_schema import VitalsPayload
from repositories.vitals_repository import VitalsRepository


class VitalsService:
    def __init__(self, repository: VitalsRepository):
        self.repository = repository
        self.anomaly_detector = VitalsAnomalyDetector()

    async def get_vitals_measure_history(self, patient_id: str, start_time: datetime, end_time: datetime):
        records = await self.repository.get_measurements(patient_id, start_time, end_time)

        return {
            "patientId": patient_id,
            "measurementsCount": len(records),
            "history": records
        }

    async def process_vitals(self, payload: VitalsPayload):
        analysis = self.anomaly_detector.analyze_vitals(payload)
        if analysis["is_anomaly"]:
            print(f'ALERT! ANOMALY DETECTED FOR PATIENT {payload.patientId}! RISK SCORE: {analysis['risk_score']}')

        await self.repository.save_measurement(payload)

        return {
            "status": "success",
            "message": "Pomiary zapisane w InfluxDB",
            "patientId": payload.patientId,
            "ai_evaluation": analysis
        }