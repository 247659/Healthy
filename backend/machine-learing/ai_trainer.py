import os
import numpy as np
import pandas as pd
import joblib
from sklearn.ensemble import IsolationForest
from datetime import datetime, timezone, timedelta

from keras.models import Sequential
from keras.layers import LSTM, Dense, RepeatVector, TimeDistributed

from vitals_repository import VitalsRepository
from data_normalizer import get_normalizer, PARAM_ORDER


class AITrainer:
    def __init__(self, repository: VitalsRepository, models_dir: str = "models/"):
        self.repository = repository
        self.models_dir = models_dir
        self.normalizer = get_normalizer()

    def _history_to_features(self, history: list) -> list[dict]:
        result = []
        for record in history:
            m = record['measurements']
            result.append({
                'heartRate':   m['heartRate'],
                'sys_bp':      m['bloodPressure']['systolic'],
                'dia_bp':      m['bloodPressure']['diastolic'],
                'temperature': m['temperature'],
                'spO2':        m['spO2'],
            })
        return result

    async def train_isolation_forest(self, patient_id: str) -> IsolationForest | None:
        end_time   = datetime.now(timezone.utc)
        start_time = end_time - timedelta(days=7)
        history    = await self.repository.get_measurements(patient_id, start_time, end_time)

        if len(history) < 50:
            print(f"[AI Trainer] Zbyt mało danych ({len(history)}/50). Przerywam trening IF.")
            return None

        features = self._history_to_features(history)

        for f in features:
            self.normalizer.update(patient_id, f)

        _, stats = self.normalizer.normalize_sequence(patient_id, features)
        norm_seq, _ = self.normalizer.normalize_sequence(patient_id, features)

        print(f"[AI Trainer] {len(history)} pomiarów, trenuję IF dla {patient_id}...")
        model = IsolationForest(n_estimators=100, contamination=0.05, random_state=42)
        model.fit(norm_seq)

        os.makedirs(self.models_dir, exist_ok=True)
        model_path = os.path.join(self.models_dir, f"{patient_id}_iforest.pkl")
        joblib.dump(model, model_path)
        print(f"[AI Trainer] Model IF zapisany: {model_path}")
        return model

    def create_sequences(self, data: np.ndarray, time_steps: int = 10) -> np.ndarray:
        X = []
        for i in range(len(data) - time_steps):
            X.append(data[i:(i + time_steps)])
        return np.array(X)

    async def train_global_lstm(self, time_steps: int = 10):
        end_time = datetime.now(timezone.utc)
        start_time = end_time - timedelta(days=7)

        chunk_size = timedelta(days=1)
        current_start = start_time

        patient_groups: dict[str, list] = {}

        print(f"[LSTM Trainer] Rozpoczynam pobieranie danych w chunkach (7 dni)...")

        while current_start < end_time:
            current_end = min(current_start + chunk_size, end_time)
            print(f"📡 Pobieranie danych: {current_start.strftime('%Y-%m-%d')} -> {current_end.strftime('%Y-%m-%d')}")

            chunk_history = await self.repository.get_all_measurements(current_start, current_end)

            for record in chunk_history:
                pid = record.get('patientId', 'global')
                m = record['measurements']
                feat = {
                    'heartRate': m['heartRate'],
                    'sys_bp': m['bloodPressure']['systolic'],
                    'dia_bp': m['bloodPressure']['diastolic'],
                    'temperature': m['temperature'],
                    'spO2': m['spO2'],
                }
                if pid not in patient_groups:
                    patient_groups[pid] = []
                patient_groups[pid].append(feat)

            current_start = current_end

        total_records = sum(len(feats) for feats in patient_groups.values())
        if total_records < 1000:
            print(f"[LSTM Trainer] Zbyt mało danych ({total_records}/1000). Przerywam.")
            return None

        all_norm = []
        for pid, feats in patient_groups.items():
            for f in feats:
                self.normalizer.update(pid, f)
            norm_seq, _ = self.normalizer.normalize_sequence(pid, feats)

            if np.isnan(norm_seq).any():
                norm_seq = norm_seq[~np.isnan(norm_seq).any(axis=1)]

            if len(norm_seq) > 0:
                all_norm.append(norm_seq)

        if not all_norm:
            print("[LSTM Trainer] Brak poprawnych danych po normalizacji.")
            return None

        data_array = np.vstack(all_norm)
        X_train = self.create_sequences(data_array, time_steps)
        print(f"[LSTM Trainer] Shape treningowy: {X_train.shape}")

        from keras import Input
        model = Sequential([
            Input(shape=(X_train.shape[1], X_train.shape[2])),
            LSTM(32, activation='relu', return_sequences=False),
            RepeatVector(X_train.shape[1]),
            LSTM(32, activation='relu', return_sequences=True),
            TimeDistributed(Dense(X_train.shape[2])),
        ])

        model.compile(optimizer='adam', loss='mse')
        print("[LSTM Trainer] Trenuję autoencoder LSTM...")
        model.fit(X_train, X_train, epochs=20, batch_size=32,
                  validation_split=0.1, verbose=1)

        os.makedirs(self.models_dir, exist_ok=True)
        model.save(os.path.join(self.models_dir, "global_lstm_model.keras"))
        print("[LSTM Trainer] Globalny model LSTM zapisany!")
        return model