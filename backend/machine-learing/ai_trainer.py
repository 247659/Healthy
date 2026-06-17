import os

import numpy as np
import pandas as pd
import joblib
from sklearn.ensemble import IsolationForest
from datetime import datetime, timezone, timedelta

from tensorflow.python.keras import Sequential
from tensorflow.python.layers.core import Dense
from keras.models import Sequential
from keras.layers import LSTM, Dense, RepeatVector, TimeDistributed
from sklearn.preprocessing import MinMaxScaler

from vitals_repository import VitalsRepository


class AITrainer:
    def __init__(self, repository: VitalsRepository, models_dir: str = "models/"):
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

    def create_sequences(self, data, time_steps=10):
        X = []
        for i in range(len(data) - time_steps):
            X.append(data[i:(i + time_steps)])
        return np.array(X)

    async def train_global_lstm(self, data_file: str):
        pass

    async def train_global_LSTM(self):
        end_time = datetime.now(timezone.utc)
        start_time = end_time - timedelta(days=7)
        history = await self.repository.get_all_measurements(start_time, end_time)
        if len(history) < 1000:
            print(f"⚠️ [LSTM Trainer] Zbyt mało danych ({len(history)}). Wymagane min. 1000.")
            return None

        raw_data = []
        for record in history:
            m = record['measurements']
            raw_data.append([
                m['heartRate'],
                m['bloodPressure']['systolic'],
                m['bloodPressure']['diastolic'],
                m['temperature'],
                m['spO2']
            ])

        data_array = np.array(raw_data)
        scaler = MinMaxScaler()
        scaled_data = scaler.fit_transform(data_array)
        time_steps = 10
        X_train = self.create_sequences(scaled_data, time_steps)
        print(f"📊 [LSTM Trainer] Kształt danych treningowych: {X_train.shape}")

        model = Sequential([
            LSTM(32, activation='relu', input_shape=(X_train.shape[1], X_train.shape[2]), return_sequences=False),
            RepeatVector(X_train.shape[1]),
            LSTM(32, activation='relu', return_sequences=True),
            TimeDistributed(Dense(X_train.shape[2]))
        ])

        model.compile(optimizer='adam', loss='mse')
        print("🧠 [LSTM Trainer] Rozpoczynam uczenie sieci. To może potrwać...")
        model.fit(X_train, X_train, epochs=20, batch_size=32, validation_split=0.1, verbose=1)
        os.makedirs(self.models_dir, exist_ok=True)
        model.save(os.path.join(self.models_dir, "global_lstm_model.keras"))
        joblib.dump(scaler, os.path.join(self.models_dir, "global_lstm_scaler.pkl"))

        print("✅ [LSTM Trainer] Globalny model LSTM gotowy i zapisany!")
        return model, scaler