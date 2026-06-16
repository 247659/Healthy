import joblib
import pandas as pd
import numpy as np
import logging
import os


class VitalsAnomalyDetector:
    def __init__(self, models_dir: str = "ai/models/"):
        self.models_dir = models_dir
        self.loaded_models = {}  # Cache w RAM
        self.global_model_path = os.path.join(models_dir, "global_isolation_forest.pkl")
        self.global_model = self._load_single_model(self.global_model_path)

    def _load_single_model(self, path: str):
        try:
            return joblib.load(path)
        except FileNotFoundError:
            return None

    # --- NOWA METODA: Sprawdzanie dostępności modelu ---
    def has_model(self, patient_id: str) -> bool:
        """Zwraca True, jeśli model pacjenta jest w RAM lub na dysku."""
        if patient_id in self.loaded_models:
            return True
        patient_model_path = os.path.join(self.models_dir, f"{patient_id}_iforest.pkl")
        return os.path.exists(patient_model_path)

    def _get_model_for_patient(self, patient_id: str):
        if patient_id in self.loaded_models:
            return self.loaded_models[patient_id]

        patient_model_path = os.path.join(self.models_dir, f"{patient_id}_iforest.pkl")
        model = self._load_single_model(patient_model_path)

        if model:
            self.loaded_models[patient_id] = model
            logging.info(f"Loaded personalized model for {patient_id}")
            return model

        return self.global_model

    def analyze_vitals(self, payload) -> dict:
        model = self._get_model_for_patient(payload.patientId)
        if model is None:
            return {"is_anomaly": False, "risk_score": 0.0}

        features = pd.DataFrame([{
            'heartRate': payload.measurements.heartRate,
            'sys_bp': payload.measurements.bloodPressure.systolic,
            'dia_bp': payload.measurements.bloodPressure.diastolic,
            'temperature': payload.measurements.temperature,
            'spO2': payload.measurements.spO2
        }])

        prediction = model.predict(features)[0]
        is_anomaly = True if prediction == -1 else False
        raw_score = model.decision_function(features)[0]
        risk_score = float(np.clip(0.5 - raw_score, 0.0, 1.0))

        return {
            "is_anomaly": is_anomaly,
            "risk_score": round(risk_score, 2),
            "personalized": payload.patientId in self.loaded_models
        }