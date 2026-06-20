from __future__ import annotations
from dataclasses import dataclass
from typing import Optional
import logging

logger = logging.getLogger("xai_explainer")

POPULATION_NORMS: dict[str, dict] = {
    "heartRate":   {"min": 60,   "max": 100,  "unit": "ud./min", "label": "Tętno"},
    "sys_bp":      {"min": 90,   "max": 140,  "unit": "mmHg",    "label": "Ciśnienie skurczowe"},
    "dia_bp":      {"min": 60,   "max": 90,   "unit": "mmHg",    "label": "Ciśnienie rozkurczowe"},
    "temperature": {"min": 36.0, "max": 37.5, "unit": "°C",      "label": "Temperatura"},
    "spO2":        {"min": 95,   "max": 100,  "unit": "%",        "label": "Saturacja (SpO2)"},
}

RISK_CRITICAL = 0.80
RISK_HIGH     = 0.60
RISK_MODERATE = 0.40


@dataclass
class VitalsSnapshot:
    heartRate:   float
    sys_bp:      float
    dia_bp:      float
    temperature: float
    spO2:        float


@dataclass
class XAIReport:
    severity:       str
    risk_score:     float
    headline:       str
    details:        list[str]
    recommendation: str
    method_used:    str
    raw_values:     dict
    forecast_note:  Optional[str] = None


def _describe_deviations(snapshot: VitalsSnapshot, patient_stats: Optional[dict] = None) -> list[str]:
    obs: list[str] = []
    values = {
        "heartRate":   snapshot.heartRate,
        "sys_bp":      snapshot.sys_bp,
        "dia_bp":      snapshot.dia_bp,
        "temperature": snapshot.temperature,
        "spO2":        snapshot.spO2,
    }
    for param, value in values.items():
        norm  = POPULATION_NORMS[param]
        label = norm["label"]
        unit  = norm["unit"]

        if patient_stats and param in patient_stats:
            mu    = patient_stats[param]["mean"]
            sigma = patient_stats[param]["std"]
            if sigma > 0:
                z = (value - mu) / sigma
                if abs(z) >= 2.5:
                    direction = "powyzej" if z > 0 else "ponizej"
                    obs.append(
                        f"{label}: {value:.1f} {unit} "
                        f"({direction} indywidualnej normy, "
                        f"oczekiwano ~{mu:.1f} {unit})"
                    )
            continue

        p_min, p_max = norm["min"], norm["max"]
        if value < p_min:
            obs.append(
                f"{label}: {value:.1f} {unit} - ponizej normy "
                f"(norma: {p_min}-{p_max} {unit}, odchylenie: {value - p_min:.1f})"
            )
        elif value > p_max:
            obs.append(
                f"{label}: {value:.1f} {unit} - powyzej normy "
                f"(norma: {p_min}-{p_max} {unit}, odchylenie: +{value - p_max:.1f})"
            )
    return obs


def _severity(risk_score: float) -> str:
    if risk_score >= RISK_CRITICAL: return "CRITICAL"
    if risk_score >= RISK_HIGH:     return "HIGH"
    if risk_score >= RISK_MODERATE: return "MODERATE"
    return "OK"


def _headline(severity: str, risk_score: float) -> str:
    pct = f"{risk_score:.0%}"
    return {
        "CRITICAL": f"ALERT KRYTYCZNY - wykryto powazna anomalie (ryzyko: {pct})",
        "HIGH":     f"Ostrzezenie - parametry zyciowe wymagaja uwagi (ryzyko: {pct})",
        "MODERATE": f"Uwaga - zaobserwowano lagodne odchylenia (ryzyko: {pct})",
        "OK":       f"Stan pacjenta stabilny (ryzyko: {pct})",
    }[severity]


def _recommendation(severity: str) -> str:
    return {
        "CRITICAL": (
            "Wymagana natychmiastowa interwencja medyczna. "
            "Skontaktuj sie z pacjentem lub wezwij pomoc."
        ),
        "HIGH": (
            "Zalecana pilna konsultacja lekarska. "
            "Monitoruj pacjenta w trybie ciaglym."
        ),
        "MODERATE": (
            "Obserwuj pacjenta. Jesli stan nie poprawi sie w ciagu 15 minut, "
            "skonsultuj sie z lekarzem."
        ),
        "OK": "Stan pacjenta w normie. Kontynuuj standardowy monitoring.",
    }[severity]


def _method_note(method_used: str) -> str:
    notes = {
        "IsolationForest": (
            "Analiza punktowa (Isolation Forest) - biezace parametry porownano "
            "z wyuczonym profilem normalnosci pacjenta."
        ),
        "LSTM": (
            "Analiza trendow (LSTM) - siec neuronowa wykryla nieprawidlowy "
            "wzorzec w przebiegu ostatnich pomiarow."
        ),
        "Hybrid (IF + LSTM)": (
            "Analiza hybrydowa (Isolation Forest + LSTM) - zarowno analiza "
            "chwilowa jak i analiza trendow wskazuja na odchylenie od normy."
        ),
        "Threshold": (
            "Analiza progowa (tryb startowy) - brak wystarczajacej historii "
            "pacjenta, uzyto ogolnych norm populacyjnych."
        ),
    }
    return notes.get(method_used, f"Metoda analizy: {method_used}")


def explain(
    snapshot: VitalsSnapshot,
    risk_score: float,
    is_anomaly: bool,
    method_used: str = "IsolationForest",
    patient_stats: Optional[dict] = None,
    lstm_forecast: Optional[dict] = None,
) -> XAIReport:
    sev          = _severity(risk_score)
    observations = _describe_deviations(snapshot, patient_stats)

    if is_anomaly and not observations:
        observations.append(
            "Wykryto anomalie wielowymiarowa - kombinacja parametrow znaczaco "
            "odbiega od wyuczonego wzorca normalnosci, choc zaden pojedynczy "
            "wskaznik nie przekracza typowego progu."
        )

    observations.append(f"[Metoda] {_method_note(method_used)}")

    forecast_note: Optional[str] = None
    if lstm_forecast:
        eta     = lstm_forecast.get("eta_minutes", "?")
        at_risk = [
            f"{POPULATION_NORMS.get(p, {}).get('label', p)}: "
            f"~{v:.1f} {POPULATION_NORMS.get(p, {}).get('unit', '')}"
            for p, v in lstm_forecast.items()
            if p != "eta_minutes"
        ]
        if at_risk:
            forecast_note = (
                f"Prognoza LSTM (za ~{eta} min): "
                + ", ".join(at_risk)
                + ". Zalecane dzialanie prewencyjne."
            )
            observations.append(f"[Prognoza] {forecast_note}")

    report = XAIReport(
        severity=sev,
        risk_score=round(risk_score, 3),
        headline=_headline(sev, risk_score),
        details=observations,
        recommendation=_recommendation(sev),
        method_used=method_used,
        raw_values={
            "heartRate":   snapshot.heartRate,
            "sys_bp":      snapshot.sys_bp,
            "dia_bp":      snapshot.dia_bp,
            "temperature": snapshot.temperature,
            "spO2":        snapshot.spO2,
            "risk_score":  round(risk_score, 3),
        },
        forecast_note=forecast_note,
    )

    logger.info(
        f"[XAI] severity={sev} risk={risk_score:.2f} "
        f"method={method_used} obs={len(observations)}"
    )
    return report
