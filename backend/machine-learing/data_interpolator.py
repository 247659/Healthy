from __future__ import annotations

import numpy as np
import logging
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Optional

logger = logging.getLogger("data_interpolator")

PHYSICAL_LIMITS: dict[str, tuple[float, float]] = {
    "heartRate":   (20,   300),
    "sys_bp":      (50,   300),
    "dia_bp":      (30,   200),
    "temperature": (30.0, 45.0),
    "spO2":        (50,   100),
}

MAX_GAP_SECONDS = 30

@dataclass
class VitalsReading:
    timestamp:    datetime
    heartRate:    float
    sys_bp:       float
    dia_bp:       float
    temperature:  float
    spO2:         float
    interpolated: bool = False
    rejected:     bool = False
    reject_reason: str = ""


def _to_dict(r: VitalsReading) -> dict:
    return {
        "heartRate":   r.heartRate,
        "sys_bp":      r.sys_bp,
        "dia_bp":      r.dia_bp,
        "temperature": r.temperature,
        "spO2":        r.spO2,
    }


def validate_reading(reading: VitalsReading) -> VitalsReading:
    values = _to_dict(reading)
    for param, (lo, hi) in PHYSICAL_LIMITS.items():
        v = values.get(param)
        if v is None:
            reading.rejected = True
            reading.reject_reason = f"Brak wartosci parametru: {param}"
            return reading
        if not (lo <= v <= hi):
            reading.rejected = True
            reading.reject_reason = (
                f"{param}={v} poza zakresem fizycznym [{lo}, {hi}]"
            )
            logger.warning(
                f"[Interpolator] Odrzucono artefakt: {reading.reject_reason}"
            )
            return reading
    return reading


def interpolate_gap(
    prev: VitalsReading,
    curr: VitalsReading,
    interval_seconds: float = 60.0,
) -> list[VitalsReading]:
    gap_seconds = (curr.timestamp - prev.timestamp).total_seconds()

    if gap_seconds <= interval_seconds:
        return []

    if gap_seconds > MAX_GAP_SECONDS:
        logger.info(
            f"[Interpolator] Przerwa {gap_seconds:.0f}s > {MAX_GAP_SECONDS}s — "
            f"nie interpoluje (za dlugie zerwanie sygnalu)"
        )
        return []

    n_missing = int(gap_seconds / interval_seconds) - 1
    if n_missing <= 0:
        return []

    params = ["heartRate", "sys_bp", "dia_bp", "temperature", "spO2"]
    prev_vals = np.array([getattr(prev, p) for p in params], dtype=float)
    curr_vals = np.array([getattr(curr, p) for p in params], dtype=float)

    interpolated: list[VitalsReading] = []
    for step in range(1, n_missing + 1):
        alpha      = step / (n_missing + 1)
        interp_val = prev_vals + alpha * (curr_vals - prev_vals)
        ts         = prev.timestamp + timedelta(seconds=interval_seconds * step)

        r = VitalsReading(
            timestamp=ts,
            heartRate=round(float(interp_val[0]), 1),
            sys_bp=round(float(interp_val[1]), 1),
            dia_bp=round(float(interp_val[2]), 1),
            temperature=round(float(interp_val[3]), 2),
            spO2=round(float(interp_val[4]), 1),
            interpolated=True,
        )
        interpolated.append(r)

    logger.info(
        f"[Interpolator] Uzupelniono {n_missing} brakujacych odczytow "
        f"(przerwa {gap_seconds:.0f}s)"
    )
    return interpolated


def process_sequence(
    readings: list[VitalsReading],
    interval_seconds: float = 60.0,
) -> list[VitalsReading]:
    if not readings:
        return []

    validated: list[VitalsReading] = []
    rejected_count = 0
    for r in readings:
        r = validate_reading(r)
        if r.rejected:
            rejected_count += 1
        else:
            validated.append(r)

    if rejected_count:
        logger.info(f"[Interpolator] Odrzucono {rejected_count} artefaktow")

    if len(validated) < 2:
        return validated

    result: list[VitalsReading] = [validated[0]]
    interpolated_count = 0

    for i in range(1, len(validated)):
        prev = validated[i - 1]
        curr = validated[i]
        filled = interpolate_gap(prev, curr, interval_seconds)
        interpolated_count += len(filled)
        result.extend(filled)
        result.append(curr)

    if interpolated_count:
        logger.info(
            f"[Interpolator] Dodano {interpolated_count} interpolowanych odczytow "
            f"(z {len(validated)} oryginalnych -> {len(result)} lacznie)"
        )

    return result


def payload_to_reading(payload, timestamp: Optional[datetime] = None) -> VitalsReading:
    ts = timestamp or payload.timestamp
    return VitalsReading(
        timestamp=ts,
        heartRate=payload.heartRate,
        sys_bp=payload.systolicBp,
        dia_bp=payload.diastolicBp,
        temperature=payload.temperature,
        spO2=payload.spO2,
    )
