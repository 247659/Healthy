# Auth Service (Keycloak)

## Opis
Serwis odpowiedzialny za uwierzytelnianie i autoryzację użytkowników w systemie HealthMonitor IoT. 

## Odpowiedzialności
* **Zarządzanie tożsamością:** Obsługa logowania pacjentów, lekarzy i administratorów.
* **Generowanie tokenów:** Wystawianie tokenów JWT z czasem ważności 24h po poprawnym logowaniu.
* **Zarządzanie uprawnieniami:** Weryfikacja ról (Pacjent, Personel medyczny, Administrator).

## Technologie
* Keycloak (obraz Docker)
* JWT