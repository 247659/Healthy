import joblib
import numpy as np
import logging
import os
from typing import Optional

try:
    from tensorflow.keras.models import load_model
    TENSORFLOW_AVAILABLE = True
except ImportError:
    TENSORFLOW_AVAILABLE = False
    logging.warning("TensorFlow niedostepny — LSTM wylaczone.")

from data_normalizer import get_normalizer, PARAM_ORDER
from xai_explainer import explain, VitalsSnapshot

logger = logging.getLogger("anomaly_detector")


class VitalsAnomalyDetector:

    def __init__(self, models_dir: str = os.getenv("MODELS_DIR", "./models/")):
        self.models_dir     = models_dir
        self.loaded_models  = {}
        self.global_model   = self._load_model(
            os.path.join(models_dir, "global_isolation_forest.pkl")
        )
        self.use_lstm       = False
        self.TIME_STEPS     = 10
        self.patient_buffers: dict[str, list] = {}
        self.normalizer     = get_normalizer()

        if TENSORFLOW_AVAILABLE:
            try:
                lstm_path   = os.path.join(models_dir, "global_lstm_model.keras")
                if os.path.exists(lstm_path):
                    self.lstm_model = load_model(lstm_path)
                    self.use_lstm   = True
                    logger.info("Globalny model LSTM zaladowany.")
                else:
                    logger.info("Brak modelu LSTM — tylko Isolation Forest.")
            except Exception as e:
                logger.error(f"Blad ladowania LSTM: {e}")

    def _load_model(self, path: str):
        try:
            return joblib.load(path)
        except FileNotFoundError:
            return None

    def has_model(self, patient_id: str) -> bool:
        if patient_id in self.loaded_models:
            return True
        return os.path.exists(
            os.path.join(self.models_dir, f"{patient_id}_iforest.pkl")
        )

    def _get_model(self, patient_id: str):
        if patient_id in self.loaded_models:
            return self.loaded_models[patient_id]
        path  = os.path.join(self.models_dir, f"{patient_id}_iforest.pkl")
        model = self._load_model(path)
        if model:
            self.loaded_models[patient_id] = model
            logger.info(f"Zaladowano model per-pacjent: {patient_id}")
            return model
        return self.global_model

    def analyze_vitals(self, payload) -> dict:
        patient_id = payload.patientId

        raw_features = {
            "heartRate":   payload.heartRate,
            "sys_bp":      payload.systolicBp,
            "dia_bp":      payload.diastolicBp,
            "temperature": payload.temperature,
            "spO2":        payload.spO2,
        }

        raw_features = {k: v for k, v in raw_features.items() if v is not None}

        self.normalizer.update(patient_id, raw_features)

        norm_vector, patient_stats = self.normalizer.normalize(patient_id, raw_features)

        model = self._get_model(patient_id)
        if model is None:
            if_risk    = 0.0
            if_anomaly = False
        else:
            norm_2d    = norm_vector.reshape(1, -1)
            prediction = model.predict(norm_2d)[0]
            if_anomaly = prediction == -1
            raw_score  = model.decision_function(norm_2d)[0]
            if_risk    = float(np.clip(0.5 - raw_score, 0.0, 1.0))

        method      = "IsolationForest"
        final_risk  = if_risk
        final_anom  = if_anomaly
        lstm_risk   = None
        lstm_forecast_for_xai: Optional[dict] = None
        if self.use_lstm:
            if patient_id not in self.patient_buffers:
                self.patient_buffers[patient_id] = []
            self.patient_buffers[patient_id].append(list(norm_vector))

            if len(self.patient_buffers[patient_id]) > self.TIME_STEPS:
                self.patient_buffers[patient_id].pop(0)

            if len(self.patient_buffers[patient_id]) == self.TIME_STEPS:
                window       = np.array(self.patient_buffers[patient_id])
                lstm_input   = np.expand_dims(window, axis=0)
                recon        = self.lstm_model.predict(lstm_input, verbose=0)
                mse          = float(np.mean(np.power(lstm_input - recon, 2)))
                lstm_risk    = float(np.clip(mse * 2, 0.0, 1.0))
                lstm_anomaly = lstm_risk > 0.7

                final_risk = max(if_risk, lstm_risk)
                final_anom = if_anomaly or lstm_anomaly
                method     = "Hybrid (IF + LSTM)"

                last_recon_norm    = recon[0, -1, :]
                denorm             = self.normalizer.denormalize(patient_id, last_recon_norm, patient_stats)
                lstm_forecast_for_xai = {p: denorm[p] for p in PARAM_ORDER}
                lstm_forecast_for_xai["eta_minutes"] = 5

        snapshot = VitalsSnapshot(
            heartRate=payload.heartRate,
            sys_bp=payload.systolicBp,
            dia_bp=payload.diastolicBp,
            temperature=payload.temperature,
            spO2=payload.spO2,
        )

        xai_stats = (
            patient_stats
            if self.normalizer.is_personalized(patient_id)
            else None
        )

        xai_report = explain(
            snapshot=snapshot,
            risk_score=final_risk,
            is_anomaly=final_anom,
            method_used=method,
            patient_stats=xai_stats,
            lstm_forecast=lstm_forecast_for_xai,
        )

        result: dict = {
            "is_anomaly":   final_anom,
            "risk_score":   round(final_risk, 2),
            "personalized": self.normalizer.is_personalized(patient_id),
            "method":       method,
            "xai": {
                "severity":       xai_report.severity,
                "headline":       xai_report.headline,
                "details":        xai_report.details,
                "recommendation": xai_report.recommendation,
                "forecast_note":  xai_report.forecast_note,
            },
        }

        if lstm_risk is not None:
            result["if_risk"]   = round(if_risk, 2)
            result["lstm_risk"] = round(lstm_risk, 2)
        else:
            lstm_buf = self.patient_buffers.get(patient_id, [])
            if self.use_lstm and len(lstm_buf) < self.TIME_STEPS:
                result["lstm_status"] = f"Buffering ({len(lstm_buf)}/{self.TIME_STEPS})"

        return result
