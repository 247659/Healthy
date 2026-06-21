import axios from 'axios';
import {authService} from "./authClient.ts";

export const apiNoAuth = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

export const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem("access_token");

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

let isRefreshing = false;
// Kolejka dla żądań, które czekają na nowy token
let failedQueue: Array<{ resolve: (value?: unknown) => void, reject: (reason?: any) => void }> = [];

const processQueue = (error: any, token: string) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

// --- INTERCEPTOR ODPOWIEDZI ---
api.interceptors.response.use(
    (response) => {
        // Jeśli odpowiedź jest poprawna, po prostu ją zwracamy
        return response;
    },
    async (error) => {
        const originalRequest = error.config;
        // Jeśli błąd to 401 i jeszcze nie ponawialiśmy tego żądania (_retry to nasza własna flaga)
        if (error.response?.status === 401 && !originalRequest._retry) {
            // Jeśli aktualnie trwa odświeżanie tokena z innego żądania,
            // dodajemy to żądanie do kolejki oczekujących
            if (isRefreshing) {
                return new Promise(function(resolve, reject) {
                    failedQueue.push({ resolve, reject });
                })
                    .then(token => {
                        originalRequest.headers['Authorization'] = 'Bearer ' + token;
                        return api(originalRequest);
                    })
                    .catch(err => {
                        return Promise.reject(err);
                    });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const refreshToken = localStorage.getItem('refresh_token');

            if (!refreshToken) {
                // Brak tokena odświeżającego - wylogowujemy natychmiast
                isRefreshing = false;
                localStorage.removeItem('access_token');
                localStorage.removeItem('refresh_token');
                window.location.href = '/'; // Przekierowanie na stronę logowania
                return Promise.reject(error);
            }

            try {
                // Wywołanie Twojego endpointu na backendzie.
                // UWAGA: Dopasuj ścieżkę '/auth/refresh' do tego, jak ją nazwałeś w kontrolerze.
                // Używamy apiNoAuth, żeby ten strzał nie zapętlił nam interceptora.
                // Zakładam, że backend zwraca: { access_token: '...', refresh_token: '...' }
                const newTokens = await authService.refreshToken(refreshToken)

                // Zapisujemy nowe tokeny
                localStorage.setItem('access_token', newTokens.accessToken);
                // Jeśli backend wydaje też nowy refresh token, zapisz go. Jeśli nie - usuń poniższą linię.
                if (newTokens.refreshToken) {
                    localStorage.setItem('refresh_token', newTokens.refreshToken);
                }

                // Aktualizujemy domyślne nagłówki oraz nagłówek aktualnego żądania
                api.defaults.headers.common['Authorization'] = 'Bearer ' + newTokens.accessToken;
                originalRequest.headers['Authorization'] = 'Bearer ' + newTokens.accessToken;

                // Uwalniamy kolejkę - mówimy czekającym żądaniom "Zdobyliśmy nowy token, możecie lecieć!"
                processQueue(null, newTokens.accessToken);

                // Ponawiamy oryginalne żądanie
                return api(originalRequest);

            } catch (err) {
                // Błąd odświeżania (np. wygasł również refresh_token) -> Twarde wylogowanie
                processQueue(err, null);
                localStorage.removeItem('access_token');
                localStorage.removeItem('refresh_token');
                window.location.href = '/'; // Wyrzucamy na login
                return Promise.reject(err);
            } finally {
                // Niezależnie od wyniku kończymy proces odświeżania
                isRefreshing = false;
            }
        }

        // Jeśli to nie jest błąd 401, np. 404 albo 500, rzucamy błędem normalnie
        return Promise.reject(error);
    }
);
