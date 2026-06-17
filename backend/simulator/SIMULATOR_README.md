PRZYKŁAD HTTP (REALTIME):
python simulator.py --mode realtime --patient-id JanKowalski --url "http://localhost:8080/api/v1/integration"

PRZYKŁAD BATCH (ZAPIS DO PLIKU):
python simulator.py --mode batch --patient-id JanKowalski --days 30

LISTA DOSTĘPNYCH PARAMETRÓW:
--mode        Wybór trybu: 'realtime' lub 'batch' (domyślnie: realtime)
--patient-id  Identyfikator pacjenta (domyślnie: patient_123) a zarazem nazwa zapisanego pliku
--interval    Czas w sekundach między pomiarami (domyślnie: 5)
--url         Adres serwera do wysyłki w trybie realtime (domyślnie: http://localhost:8080/api/v1/integration)
--days        Liczba dni wstecz do wygenerowania w trybie batch (domyślnie: 1)
--file        Nazwa pliku w trybie batch (domyślnie: [patient-id].jsonl)