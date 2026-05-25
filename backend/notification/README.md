# Alert and Notification Service

## Opis
System powiadomień. Odbiera sygnały o anomaliach z `machine-learning` service i dba o to, aby ostrzeżenie dotarło do odpowiednich osób.

## Odpowiedzialności
* **Dystrybucja alertów:** Wysyłanie alarmów krytycznych oraz pre-alertów do pacjentów i personelu medycznego.
* **Przechowywanie historii zdarzeń:** Rejestrowanie każdego alertu wraz z wyjaśnieniem algorytmu (tzw. XAI - Explainable AI).
* **Komunikacja z interfejsem:** Wystawianie endpointów dla aplikacji mobilnej i webowej do pobierania aktywnych powiadomień.

## Technologie
* Spring Boot
* PostgreSQL
* RabbitMQ / Apache Kafka (klient)
* Docker