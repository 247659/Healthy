import joblib
import pandas as pd
import numpy as np
import logging

class VitalsAnomalyDetector:
    def __init__(self, model_path: str = "ai/models/isolation_forest.pkl"):
        self.model_path = model_path
        self.model = None
        self._load_model()

    def _load_model(self):
        try:
            self.model = joblib.load(self.model_path)
            logging.info(f"ML model loaded successfully from {self.model_path}")
        except FileNotFoundError:
            logging.warning("ML model not found")

    def analyze_vitals(self, payload) -> dict:
        if self.model is None:
            return {"is anomaly": False, "risk_score": 0.0}

        features = pd.DataFrame([{
            'heartRate': payload.measurements.heartRate,
            'sys_bp': payload.measurements.bloodPressure.systolic,
            'dia_bp': payload.measurements.bloodPressure.diastolic,
            'temperature': payload.measurements.temperature,
            'spO2': payload.measurements.spO2
        }])

        prediction = self.model.predict(features)[0]
        is_anomaly = True if prediction == -1 else False
        raw_score = self.model.decision_function(features)[0]
        risk_score = float(np.clip(0.5 - raw_score, 0.0, 1.0))
        return {
            "is_anomaly": is_anomaly,
            "risk_score": round(risk_score, 2)
        }