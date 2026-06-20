import joblib
import pandas as pd
import numpy as np
import logging
import os

try:
    import tensorflow.keras.models

    TENSORFLOW_AVAILABLE = True
except ImportError:
    TENSORFLOW_AVAILABLE = False
    logging.warning("TensorFlow nie jest zainstalowany. LSTM będzie wyłączone.")


class VitalsAnomalyDetector:
    def __init__(self, models_dir: str = "/models/"):
        self.models_dir = models_dir
        self.loaded_models = {}
        self.global_model_path = os.path.join(models_dir, "global_isolation_forest.pkl")
        self.global_model = self._load_single_model(self.global_model_path)
        self.use_lstm = False
        self.TIME_STEPS = 10
        self.patient_buffers = {}

        if TENSORFLOW_AVAILABLE:
            try:
                lstm_path = os.path.join(models_dir, "global_lstm_model.keras")
                scaler_path = os.path.join(models_dir, "global_lstm_scaler.pkl")

                if os.path.exists(lstm_path) and os.path.exists(scaler_path):
                    self.lstm_model = load_model(lstm_path)
                    self.lstm_scaler = joblib.load(scaler_path)
                    self.use_lstm = True
                    logging.info("✅ Globalny model LSTM pomyślnie załadowany!")
                else:
                    logging.info("⚠️ Brak modelu LSTM na dysku. Działam tylko na Isolation Forest.")
            except Exception as e:
                logging.error(f"❌ Błąd podczas ładowania LSTM: {e}")

    def _load_single_model(self, path: str):
        try:
            return joblib.load(path)
        except FileNotFoundError:
            return None

    def has_model(self, patient_id: str) -> bool:
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
            logging.info(f"Załadowano model z dysku dla {patient_id}")
            return model

        return self.global_model

    def analyze_vitals(self, payload) -> dict:
        patient_id = payload.patientId
        model = self._get_model_for_patient(patient_id)

        features_df = pd.DataFrame([{
            'heartRate': payload.heartRate,
            'sys_bp': payload.systolicBp,
            'dia_bp': payload.diastolicBp,
            'temperature': payload.temperature,
            'spO2': payload.spO2
        }])

        if model is None:
            if_risk_score = 0.0
            if_is_anomaly = False
        else:
            prediction = model.predict(features_df)[0]
            if_is_anomaly = True if prediction == -1 else False
            raw_score = model.decision_function(features_df)[0]
            if_risk_score = float(np.clip(0.5 - raw_score, 0.0, 1.0))

        result = {
            "is_anomaly": if_is_anomaly,
            "risk_score": round(if_risk_score, 2),
            "personalized": patient_id in self.loaded_models,
            "method": "IsolationForest"
        }

        if self.use_lstm:
            current_features = [
                payload.heartRate,
                payload.systolicBp,
                payload.diastolicBp,
                payload.temperature,
                payload.spO2
            ]

            if patient_id not in self.patient_buffers:
                self.patient_buffers[patient_id] = []

            self.patient_buffers[patient_id].append(current_features)

            if len(self.patient_buffers[patient_id]) > self.TIME_STEPS:
                self.patient_buffers[patient_id].pop(0)

            if len(self.patient_buffers[patient_id]) == self.TIME_STEPS:
                window = np.array(self.patient_buffers[patient_id])

                scaled_window = self.lstm_scaler.transform(window)

                lstm_input = np.expand_dims(scaled_window, axis=0)

                reconstruction = self.lstm_model.predict(lstm_input, verbose=0)

                mse = np.mean(np.power(lstm_input - reconstruction, 2), axis=1)[0]

                lstm_risk_score = float(np.clip(mse * 10, 0.0, 1.0))
                lstm_is_anomaly = lstm_risk_score > 0.7

                final_risk = max(if_risk_score, lstm_risk_score)
                final_anomaly = if_is_anomaly or lstm_is_anomaly

                result.update({
                    "is_anomaly": final_anomaly,
                    "risk_score": round(final_risk, 2),
                    "if_risk": round(if_risk_score, 2),
                    "lstm_risk": round(lstm_risk_score, 2),
                    "method": "Hybrid (IF + LSTM)"
                })
            else:
                result["lstm_status"] = f"Buffering ({len(self.patient_buffers[patient_id])}/{self.TIME_STEPS})"

        return result