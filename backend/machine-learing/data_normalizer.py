from __future__ import annotations

import numpy as np
import logging
from collections import deque
from typing import Optional

logger = logging.getLogger("data_normalizer")

POPULATION_DEFAULTS: dict[str, dict] = {
    "heartRate":   {"mean": 75.0,  "std": 12.0},
    "sys_bp":      {"mean": 120.0, "std": 15.0},
    "dia_bp":      {"mean": 75.0,  "std": 10.0},
    "temperature": {"mean": 36.6,  "std": 0.4},
    "spO2":        {"mean": 98.0,  "std": 1.5},
}

PARAM_ORDER = ["heartRate", "sys_bp", "dia_bp", "temperature", "spO2"]

MIN_SAMPLES = 50


class PatientNormalizer:

    def __init__(self, max_window: int = 2880):
        self.max_window = max_window
        # {patient_id: {param: deque of values}}
        self._buffers: dict[str, dict[str, deque]] = {}

    def update(self, patient_id: str, features: dict[str, float]) -> None:
        if patient_id not in self._buffers:
            self._buffers[patient_id] = {
                param: deque(maxlen=self.max_window) for param in PARAM_ORDER
            }
        for param in PARAM_ORDER:
            if param in features and features[param] is not None:
                self._buffers[patient_id][param].append(features[param])


    def get_stats(self, patient_id: str) -> dict[str, dict[str, float]]:
        stats: dict[str, dict[str, float]] = {}
        buf = self._buffers.get(patient_id, {})

        for param in PARAM_ORDER:
            values = list(buf.get(param, []))
            if len(values) >= MIN_SAMPLES:
                arr = np.array(values)
                std = float(np.std(arr))
                stats[param] = {
                    "mean":         float(np.mean(arr)),
                    "std":          std if std > 0 else 0.01,
                    "personalized": True,
                    "n_samples":    len(values),
                }
            else:
                defaults = POPULATION_DEFAULTS[param]
                stats[param] = {
                    "mean":         defaults["mean"],
                    "std":          defaults["std"],
                    "personalized": False,
                    "n_samples":    len(values),
                }
        return stats

    def sample_count(self, patient_id: str, param: str = "heartRate") -> int:
        buf = self._buffers.get(patient_id, {})
        return len(buf.get(param, []))

    def normalize(
        self,
        patient_id: str,
        features: dict[str, float],
    ) -> tuple[np.ndarray, dict]:
        stats  = self.get_stats(patient_id)
        result = np.zeros(len(PARAM_ORDER), dtype=np.float32)
        for i, param in enumerate(PARAM_ORDER):
            raw_val = features.get(param)
            raw = raw_val if raw_val is not None else stats[param]["mean"]

            mu   = stats[param]["mean"]
            sigma = stats[param]["std"]
            result[i] = (raw - mu) / sigma
        return result, stats

    def normalize_sequence(
        self,
        patient_id: str,
        sequence: list[dict[str, float]],
    ) -> tuple[np.ndarray, dict]:
        stats  = self.get_stats(patient_id)
        result = np.zeros((len(sequence), len(PARAM_ORDER)), dtype=np.float32)
        for t, reading in enumerate(sequence):
            for i, param in enumerate(PARAM_ORDER):
                raw_val = reading.get(param)

                raw   = raw_val if raw_val is not None else stats[param]["mean"]
                mu    = stats[param]["mean"]
                sigma = stats[param]["std"]
                result[t, i] = (raw - mu) / sigma
        return result, stats

    def denormalize(
        self,
        patient_id: str,
        normalized: np.ndarray,
        stats: Optional[dict] = None,
    ) -> dict[str, float]:
        if stats is None:
            stats = self.get_stats(patient_id)

        if normalized.ndim == 1:
            result = {}
            for i, param in enumerate(PARAM_ORDER):
                mu    = stats[param]["mean"]
                sigma = stats[param]["std"]
                result[param] = float(normalized[i] * sigma + mu)
            return result

        result = {}
        for i, param in enumerate(PARAM_ORDER):
            mu    = stats[param]["mean"]
            sigma = stats[param]["std"]
            result[param] = float(normalized[-1, i] * sigma + mu)
        return result

    def is_personalized(self, patient_id: str) -> bool:
        return self.sample_count(patient_id) >= MIN_SAMPLES

_normalizer: Optional[PatientNormalizer] = None


def get_normalizer() -> PatientNormalizer:
    global _normalizer
    if _normalizer is None:
        _normalizer = PatientNormalizer()
    return _normalizer
