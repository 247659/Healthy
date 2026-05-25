# Vital Signs Service

## Opis
Główne archiwum danych medycznych. Serwis zajmuje się trwałym zapisem spływających, poprawnych pomiarów oraz ich serwowaniem dla innych usług.

## Odpowiedzialności
* **Trwały zapis:** Nasłuchiwanie na kolejce wiadomości i zapisywanie przefiltrowanych danych z `iot-integration` w bazie danych szeregów czasowych.
* **Udostępnianie historii:** Wystawianie API (np. `GET /api/vitals/{id}`) pozwalającego na rysowanie wykresów w aplikacjach klienckich.
* **Przygotowanie danych pod analizę:** Udostępnianie historycznych okien danych dla modułu `machine-learning`.

## Technologie
* Spring Boot
* Baza danych szeregów czasowych (np. InfluxDB lub relacyjna z odpowiednimi indeksami)
* Docker