import json
import pandas as pd
import joblib
import os
from sklearn.ensemble import IsolationForest

def train_isolation_forest(data_file: str, model_output_path: str):
    data = []
    with open(data_file, 'r', encoding='utf-8') as f:
        for line in f:
            data.append(json.loads(line.strip()))

    df = pd.json_normalize(data)
    features = df[[
            'measurements.heartRate',
            'measurements.bloodPressure.systolic',
            'measurements.bloodPressure.diastolic',
            'measurements.temperature',
            'measurements.spO2'
        ]]

    features.columns = ['heartRate', 'sys_bp', 'dia_bp', 'temperature', 'spO2']
    model = IsolationForest(n_estimators=100, contamination=0.05, random_state=42)
    model.fit(features)
    os.makedirs(os.path.dirname(model_output_path), exist_ok=True)
    joblib.dump(model, model_output_path)

train_isolation_forest(
        data_file="patient_123.jsonl",
        model_output_path="models/isolation_forest.pkl"
    )
