import os
import pandas as pd
import joblib
from sklearn.ensemble import IsolationForest
from datetime import datetime, timezone, timedelta
from repositories.vitals_repository import VitalsRepository


class AITrainer:
    def __init__(self, repository: VitalsRepository, models_dir: str = "ai/models/"):
        self.repository = repository
        self.models_dir = models_dir

    async def train_isolation_forest(self, patient_id: str) -> IsolationForest | None:
        end_time = datetime.now(timezone.utc)
        start_time = end_time - timedelta(days=7)
        history = await self.repository.get_measurements(patient_id, start_time, end_time)
        if len(history) < 50:
            print(f"[AI Trainer] Zbyt mało danych ({len(history)}/50). Przerywam trening.")
            return None

        raw_data = []
        for record in history:
            m = record['measurements']
            raw_data.append({
                'heartRate': m['heartRate'],
                'sys_bp': m['bloodPressure']['systolic'],
                'dia_bp': m['bloodPressure']['diastolic'],
                'temperature': m['temperature'],
                'spO2': m['spO2']
            })

        df = pd.DataFrame(raw_data)
        print(f"[AI Trainer] Zebrano {len(history)} pomiarów. Trenowanie modelu...")
        model = IsolationForest(n_estimators=100, contamination=0.05, random_state=42)
        model.fit(df)
        os.makedirs(self.models_dir, exist_ok=True)
        model_path = os.path.join(self.models_dir, f"{patient_id}_iforest.pkl")
        joblib.dump(model, model_path)
        return model