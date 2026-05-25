# API Gateway

## Opis
Główny punkt wejścia (brama) do systemu HealthMonitor IoT. Serwis ten przyjmuje wszystkie żądania od klientów (aplikacja mobilna, panel webowy) i przekierowuje je do odpowiednich mikrousług wewnętrznych.

## Odpowiedzialności
* **Routing żądań:** Przekierowywanie ruchu HTTP do docelowych serwisów.
* **Wstępna walidacja:** Weryfikacja obecności tokenów autoryzacyjnych przed wpuszczeniem żądania do sieci wewnętrznej.

## Technologie
* Spring Boot
* Spring Cloud Gateway
* Docker