# Machine Learning (AI) Service

## Opis
Moduł SI Detekcji Anomalii Medycznych. Jego głównym zadaniem jest ciągła analiza spływających danych medycznych i wyliczanie ryzyka dla zdrowia pacjenta.

## Odpowiedzialności
* **Analiza w czasie rzeczywistym:** Wykorzystanie algorytmu Isolation Forest do wykrywania nagłych anomalii (np. nagły spadek ciśnienia).
* **Wczesne ostrzeganie:** Wykorzystanie sieci LSTM do analizy trendów historycznych i przewidywania pogorszenia stanu zdrowia w przyszłości.
* **Wyliczanie Wskaźnika Ryzyka:** Generowanie score w przedziale [0,1] dla bieżących odczytów.

## Technologie
* Python (FastAPI / Flask)
* Scikit-Learn (Isolation Forest)
* TensorFlow / Keras (LSTM)
* Docker