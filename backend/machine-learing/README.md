# Machine Learning (AI) Service

## Opis
Moduł SI Detekcji Anomalii Medycznych. Jego głównym zadaniem jest asynchroniczna, ciągła analiza spływających danych medycznych i wyliczanie ryzyka dla zdrowia pacjenta w architekturze opartej na zdarzeniach (Event-Driven).

## Odpowiedzialności
* **Analiza w czasie rzeczywistym:** Wykorzystanie spersonalizowanych modeli algorytmu Isolation Forest do wykrywania nagłych anomalii (np. nieoczekiwany spadek ciśnienia względem normy danego pacjenta).
* **Wczesne ostrzeganie:** Wykorzystanie globalnej sieci głębokiej LSTM (Autoencoder) do analizy trendów w oknach czasowych i przewidywania kryzysów na podstawie historycznych przebiegów chorobowych.
* **Wyliczanie Wskaźnika Ryzyka:** Generowanie pesymistycznego wyniku ryzyka w przedziale [0,1] dla bieżących odczytów i automatyczne publikowanie alertów na odpowiednią kolejkę RabbitMQ w przypadku przekroczenia progu.

## Technologie
* Python (3.10+)
* Scikit-Learn (Machine Learning: Isolation Forest)
* TensorFlow / Keras (Deep Learning: LSTM Autoencoder)
* aio-pika (Asynchroniczna komunikacja z RabbitMQ)
* InfluxDB Async Client (Pobieranie historii szeregów czasowych)
* Docker

---

## Architektura Uczenia (MLOps)
System wykorzystuje podejście hybrydowe. Cykl życia obu modeli jest całkowicie od siebie oddzielony ze względów wydajnościowych:

1. **Izolowane modele pacjentów (Isolation Forest)** * Uczą się **automatycznie w tle**.
   * Gdy pacjent prześle określoną liczbę nowych pomiarów (np. 50), główny worker asynchronicznie pobiera jego ostatnią historię i aktualizuje w RAM-ie spersonalizowany model statystyczny, nie blokując przy tym kolejki głównej.

2. **Globalny model głęboki (LSTM)** * Ze względu na zapotrzebowanie na zasoby (RAM/CPU/VRAM), sieć neuronowa **nie uczy się "w locie"**. 
   * Model trenowany jest na potężnym zbiorze danych zagregowanym od wszystkich pacjentów. Trenowanie odbywa się poprzez uruchomienie dedykowanego skryptu (tzw. Batch Job). W środowisku produkcyjnym skrypt ten może być wyzwalany z poziomu CI/CD lub harmonogramu (Cron) podczas najniższego obciążenia serwerów.

---

## Uruchamianie krok po kroku

### 1. Wymagania
Upewnij się, że infrastruktura bazodanowa i broker wiadomości (RabbitMQ, InfluxDB) są uruchomione za pomocą `docker-compose`. Następnie w wirtualnym środowisku Pythona zainstaluj zależności:
```bash
pip install -r requirements.txt
```
## ⚙️ Architektura Trenowania Modeli (MLOps)

System został zaprojektowany z myślą o wysokiej wydajności i odporności na przeciążenia. Z tego powodu cykl życia modeli (ich uczenie) został podzielony na dwa niezależne procesy:

### 1. Spersonalizowane algorytmy płytkie (Isolation Forest)
* **Kiedy się uczą?** W locie (Online Learning / Background Task).
* **Jak to działa?** Główny proces `ai_worker.py`, który nasłuchuje wiadomości z RabbitMQ, liczy komunikaty od każdego pacjenta. Gdy pacjent prześle określoną partię nowych pomiarów (np. 50), worker asynchronicznie, w tle (nie blokując kolejki głównej) dotrenowuje jego spersonalizowany model na podstawie najnowszych danych. 

### 2. Globalna sieć głęboka (LSTM Autoencoder)
* **Kiedy się uczy?** Poza głównym cyklem aplikacji (Batch Processing).
* **Jak to działa?** Ze względu na ogromne zapotrzebowanie na zasoby obliczeniowe (CPU/RAM), sieć neuronowa **nie jest trenowana w głównym workerze**. Służy do tego dedykowany, osobny skrypt `train_lstm_job.py`. Uruchamia się go niezależnie (np. ręcznie przez administratora lub automatycznie przez systemowy Cron/CI pipeline w godzinach nocnych). Skrypt pobiera tysiące rekordów od *wszystkich* pacjentów, wylicza globalne wagi dla sieci i zapisuje gotowy plik modelu na dysku. 

Gdy nowy plik modelu LSTM pojawi się w systemie plików, działający w tle `ai_worker.py` automatycznie zacznie z niego korzystać do wczesnego ostrzegania o anomaliach czasowych.