from datetime import datetime
from fastapi import BackgroundTasks

from schemas.vitals_schema import VitalsPayload
from repositories.vitals_repository import VitalsRepository
from ai.anomaly_detector import VitalsAnomalyDetector
from ai.ai_trainer import AITrainer


class VitalsService:
    def __init__(self, repository: VitalsRepository):
        self.repository = repository
        self.anomaly_detector = VitalsAnomalyDetector()
        self.ai_trainer = AITrainer(repository)
        self.message_counters = {}

    async def process_vitals(self, payload: VitalsPayload, background_tasks: BackgroundTasks):
        patient_id = payload.patientId
        analysis = self.anomaly_detector.analyze_vitals(payload)
        await self.repository.save_measurement(payload)

        self.message_counters[patient_id] = self.message_counters.get(patient_id, 0) + 1
        model_exists = self.anomaly_detector.has_model(patient_id)

        if not model_exists or self.message_counters[patient_id] % 500 == 0:
            background_tasks.add_task(self._run_training_task, patient_id)

        return {
            "status": "success",
            "patientId": patient_id,
            "ai_evaluation": analysis
        }

    async def _run_training_task(self, patient_id: str):
        new_model = await self.ai_trainer.train_isolation_forest(patient_id)
        if new_model:
            self.anomaly_detector.loaded_models[patient_id] = new_model

    async def get_vitals_measure_history(self, patient_id: str, start_time: datetime, end_time: datetime):
        records = await self.repository.get_measurements(patient_id, start_time, end_time)
        return {"patientId": patient_id, "measurementsCount": len(records), "history": records}