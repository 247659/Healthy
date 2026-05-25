# IoT Integration Service

## Opis
Mikrousługa odpowiedzialna za komunikację ze światem zewnętrznym – odbiera dane z fizycznych czujników medycznych.

## Odpowiedzialności
* **Odbiór danych:** Przyjmowanie surowych pakietów JSON z pomiarami (Tętno, Ciśnienie, Temperatura, Saturacja).
* **Normalizacja i Walidacja:** Sprawdzanie, czy dane są w poprawnym formacie i odrzucanie błędnych odczytów.
* **Wysyłka na kolejkę:** Przesyłanie sprawdzonych i znormalizowanych pomiarów do wewnętrznego brokera wiadomości w celu dalszego przetwarzania.

## Technologie
* Spring Boot
* REST API / MQTT
* RabbitMQ / Apache Kafka (klient)
* Docker