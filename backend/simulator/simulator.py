import time
import json
import random
import requests
import argparse
import logging
from datetime import datetime, timezone, timedelta

KEYCLOAK_URL = "http://localhost:9090/realms/healthmonitor-realm/protocol/openid-connect/token"
CLIENT_ID = "iot-device-simulator"
CLIENT_SECRET = "xpbccFrxtYy4Fyrb5HdqK2EjjQ7hOPZR"

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


def get_access_token():
    """Pobiera Token JWT z Keycloaka dla urządzenia (M2M)"""
    payload = {
        "grant_type": "client_credentials",
        "client_id": CLIENT_ID,
        "client_secret": CLIENT_SECRET
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}

    try:
        response = requests.post(KEYCLOAK_URL, data=payload, headers=headers)
        response.raise_for_status()
        return response.json().get("access_token")
    except Exception as e:
        logging.error(f"Nie udało się pobrać tokena z Keycloak: {e}")
        return None

def run_realtime(args):
    """Wysyłanie danych na żywo co X sekund przez HTTP"""
    logging.info(f"--- TRYB REALTIME ---")
    logging.info(f"Pacjent: {args.patient_id} | Interwał: {args.interval}s | Cel: {args.url}")
    
    patient = PatientSimulator(args.patient_id)

    token = get_access_token()
    if not token:
        logging.error("Zatrzymanie symulatora z powodu braku autoryzacji.")
        return

    # 2. Tworzymy nagłówek z tokenem
    api_headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    try:
        while True:
            # ZMIANA: Nie losujemy tu anomalii! Pacjent jest "żywy" i sam steruje swoim stanem
            payload = patient.generate_vitals()
            
            # Odczytujemy z paczki, czy pacjent akurat teraz choruje (dla logów)
            is_anomaly = payload["isAnomaly"]
            
            try:
                response = requests.post(args.url, json=payload, timeout=2, headers=api_headers)
                if response.status_code in [200, 201, 202]:
                    # Wyświetlamy [ALARM!] na czerwono (w konsoli będzie czytelniej), jeśli jest anomalia
                    # status_text = "[ALARM!] Pogorszenie stanu!" if is_anomaly else "Wysłano dane (Norma)"
                    logging.info(f"Wysłano dane. Status: {response.status_code}")
                elif response.status_code == 401:
                    logging.warning("Token wygasł! Próbuję pobrać nowy...")
                    token = get_access_token()  # Odświeżenie tokena, jeśli wygasł
                    api_headers["Authorization"] = f"Bearer {token}"
                else:
                    logging.warning(f"Serwer zwrócił status: {response.status_code}")
                    
            except requests.exceptions.RequestException:
                logging.error(f"Nie można połączyć z {args.url}. Czy serwer działa?")

            time.sleep(args.interval)

    except KeyboardInterrupt:
        logging.info("Zatrzymano symulator (Realtime).")


def run_batch(args):
    """Generowanie danych historycznych do pliku JSONL (strumieniowo, oszczędza RAM)"""
    
    # --- ZMIANA: Automatyczna nazwa pliku na podstawie ID pacjenta ---
    filename = args.file if args.file else f"{args.patient_id}.jsonl"
    
    logging.info(f"--- TRYB BATCH ---")
    logging.info(f"Generowanie danych z {args.days} dni dla pacjenta {args.patient_id}...")
    
    end_time = datetime.now(timezone.utc)
    current_time = end_time - timedelta(days=args.days)
    
    patient = PatientSimulator(args.patient_id)
    anomaly_count = 0
    total_records = 0
    
    with open(filename, "w", encoding="utf-8") as f:
        while current_time < end_time:
            payload = patient.generate_vitals(timestamp=current_time)
            
            if payload["isAnomaly"]:
                anomaly_count += 1
                
            f.write(json.dumps(payload) + "\n")
            
            total_records += 1
            current_time += timedelta(seconds=args.interval)

    logging.info(f"[SUKCES] Wygenerowano {total_records} rekordów.")
    logging.info(f"W tym pomiarów z trwającą anomalią: {anomaly_count} (~{(anomaly_count/total_records)*100:.1f}%)")
    logging.info(f"Zapisano do pliku JSONL: {filename}")


def main():
    parser = argparse.ArgumentParser(description="Symulator IoT dla parametrów życiowych")
    parser.add_argument('--mode', choices=['realtime', 'batch'], default='realtime', 
                        help="Tryb działania: 'realtime' (HTTP) lub 'batch' (zapis do pliku)")
    parser.add_argument('--patient-id', type=str, default='patient_1234',
                        help="ID pacjenta")
    parser.add_argument('--url', type=str, default='http://localhost:8000/api/vitals',
                        help="Adres URL dla trybu realtime")
    parser.add_argument('--interval', type=int, default=2,
                        help="Interwał w sekundach między pomiarami")
    parser.add_argument('--days', type=int, default=1, 
                        help="Ile dni wstecz wygenerować w trybie batch")
    
    # --- ZMIANA: Usunęliśmy domyślną nazwę pliku, żeby kod mógł sam ją wygenerować z ID pacjenta ---
    parser.add_argument('--file', type=str, default=None, 
                        help="Opcjonalna nazwa pliku (domyślnie: [patient-id].jsonl)")
    
    args = parser.parse_args()
    
    if args.mode == 'realtime':
        run_realtime(args)
    elif args.mode == 'batch':
        run_batch(args)

if __name__ == "__main__":
    main()