import time
import json
import random
import requests
import argparse
import logging
from datetime import datetime, timezone, timedelta
import os

# Konfiguracja loggera
logging.basicConfig(level=logging.INFO, format='[%(asctime)s] %(levelname)s: %(message)s', datefmt='%Y-%m-%d %H:%M:%S')


class PatientSimulator:
    def __init__(self, patient_id):
        self.patient_id = patient_id

        # Fizjologiczne wartości początkowe
        self.hr = 75.0
        self.sys_bp = 120.0
        self.dia_bp = 80.0
        self.temp = 36.6
        self.spo2 = 98.0

        # Stan pacjenta (Maszyna stanowa)
        self.is_deteriorating = False
        self.ticks_in_anomaly = 0
        self.anomaly_duration = 0

    def _update_vital(self, current, target, max_drift, noise_level):
        diff = target - current

        drift = diff * 0.02
        drift = max(-max_drift, min(max_drift, drift))

        noise = random.uniform(-noise_level, noise_level)

        return current + drift + noise

    def generate_vitals(self, timestamp=None):
        if timestamp is None:
            timestamp = datetime.now(timezone.utc)

        if not self.is_deteriorating:
            if random.random() < 0.005:
                self.is_deteriorating = True
                self.anomaly_duration = random.randint(120, 360)
                self.ticks_in_anomaly = 0
        else:
            self.ticks_in_anomaly += 1
            if self.ticks_in_anomaly >= self.anomaly_duration:
                self.is_deteriorating = False

        if self.is_deteriorating:
            target_hr = 165.0
            target_sys = 180.0
            target_dia = 110.0
            target_temp = 39.5
            target_spo2 = 85.0
        else:
            target_hr = 75.0
            target_sys = 120.0
            target_dia = 80.0
            target_temp = 36.6
            target_spo2 = 98.0

        self.hr = self._update_vital(self.hr, target_hr, max_drift=1.0, noise_level=2.0)
        self.sys_bp = self._update_vital(self.sys_bp, target_sys, max_drift=1.0, noise_level=1.5)
        self.dia_bp = self._update_vital(self.dia_bp, target_dia, max_drift=0.5, noise_level=1.0)
        self.temp = self._update_vital(self.temp, target_temp, max_drift=0.02, noise_level=0.05)
        self.spo2 = self._update_vital(self.spo2, target_spo2, max_drift=0.2, noise_level=0.5)

        self.spo2 = min(100.0, self.spo2)

        return {
            "patientId": self.patient_id,
            "timestamp": timestamp.isoformat(),
            "measurements": {
                "heartRate": int(self.hr),
                "bloodPressure": {
                    "systolic": int(self.sys_bp),
                    "diastolic": int(self.dia_bp)
                },
                "temperature": round(self.temp, 1),
                "spO2": int(self.spo2)
            },
            "isAnomaly": self.is_deteriorating
        }


def run_realtime(args):
    """Wysyłanie danych na żywo co X sekund przez HTTP"""
    logging.info(f"--- TRYB REALTIME ---")
    logging.info(f"Pacjent: {args.patient_id} | Interwał: {args.interval}s | Cel: {args.url}")

    patient = PatientSimulator(args.patient_id)

    # Nagłówek dla zwykłego JSON
    api_headers = {"Content-Type": "application/json"}

    try:
        while True:
            payload = patient.generate_vitals()
            is_anomaly = payload["isAnomaly"]

            try:
                response = requests.post(args.url, json=payload, timeout=2, headers=api_headers)
                if response.status_code in [200, 201, 202]:
                    logging.info(f"Wysłano dane (Anomalia: {is_anomaly}). Status: {response.status_code}")
                else:
                    logging.warning(f"Serwer zwrócił status: {response.status_code} - {response.text}")

            except requests.exceptions.RequestException:
                logging.error(f"Nie można połączyć z {args.url}. Czy serwer działa?")

            time.sleep(args.interval)

    except KeyboardInterrupt:
        logging.info("Zatrzymano symulator (Realtime).")


def run_batch(args):
    """Generowanie danych historycznych do pliku JSONL i wysyłka multipart/form-data do serwera"""
    filename = args.file if args.file else f"{args.patient_id}.jsonl"

    logging.info(f"--- TRYB BATCH ---")
    logging.info(f"Generowanie danych z {args.days} dni dla pacjenta {args.patient_id}...")

    end_time = datetime.now(timezone.utc)
    current_time = end_time - timedelta(days=args.days)

    patient = PatientSimulator(args.patient_id)
    anomaly_count = 0
    total_records = 0

    # KROK 1: Generowanie pliku na dysk
    with open(filename, "w", encoding="utf-8") as f:
        while current_time < end_time:
            payload = patient.generate_vitals(timestamp=current_time)
            if payload["isAnomaly"]:
                anomaly_count += 1
            f.write(json.dumps(payload) + "\n")
            total_records += 1
            current_time += timedelta(seconds=args.interval)

    logging.info(f"[SUKCES] Wygenerowano {total_records} rekordów do pliku: {filename}")

    # KROK 2: Wysłanie pliku do Integration Service
    batch_url = f"{args.url}/batch"

    # Pobranie rozmiaru pliku w bajtach
    file_size_bytes = os.path.getsize(filename)
    file_size_mb = file_size_bytes / (1024 * 1024)

    logging.info(f"Wysyłam plik wsadowy ({filename}, rozmiar: {file_size_mb:.2f} MB) do serwera: {batch_url}")
    try:
        with open(filename, 'rb') as f:
            files = {'file': (filename, f, 'application/x-ndjson')}
            response = requests.post(batch_url, files=files)

        if response.status_code in [200, 201, 202, 203, 204]:
            logging.info(f"Plik pomyślnie przyjęty przez serwer. Status: {response.status_code}")
        else:
            logging.error(f"Błąd wysyłania. Serwer zwrócił: {response.status_code} - {response.text}")
    except requests.exceptions.RequestException as e:
        logging.error(f"Nie można połączyć z {batch_url}. Błąd: {e}")


def main():
    parser = argparse.ArgumentParser(description="Symulator IoT dla parametrów życiowych")
    parser.add_argument('--mode', choices=['realtime', 'batch'], default='realtime',
                        help="Tryb działania: 'realtime' (HTTP) lub 'batch' (zapis do pliku i wysyłka batch)")
    parser.add_argument('--patient-id', type=str, default='35ccfead-5a9f-4784-ab78-4bdb25aee903',
                        help="ID pacjenta")

    default_api_url = os.getenv("API_URL", "http://localhost:8083/api/v1/integration")

    parser.add_argument('--url', type=str, default=default_api_url,
                        help="Adres URL")
    parser.add_argument('--interval', type=int, default=2,
                        help="Interwał w sekundach między pomiarami")
    parser.add_argument('--days', type=int, default=1,
                        help="Ile dni wstecz wygenerować w trybie batch")
    parser.add_argument('--file', type=str, default=None,
                        help="Opcjonalna nazwa pliku")

    args = parser.parse_args()

    if args.mode == 'realtime':
        run_realtime(args)
    elif args.mode == 'batch':
        run_batch(args)


if __name__ == "__main__":
    main()