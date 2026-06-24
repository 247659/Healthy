import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

export const apiClient = axios.create({
    baseURL: 'http://10.0.2.2:8080/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

// --- INTERCEPTOR ŻĄDANIA ---
// Uruchamia się PRZED wysłaniem każdego requestu
apiClient.interceptors.request.use(
    async (config) => {
        // Pobierz token z pamięci urządzenia
        const token = await AsyncStorage.getItem('accessToken');

        // Jeśli token istnieje, doklej go do nagłówka Authorization
        if (token && config.headers) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// --- INTERCEPTOR ODPOWIEDZI ---
// Uruchamia się PO otrzymaniu odpowiedzi z serwera (przechwytuje błędy)
apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        // Sprawdzamy czy błąd to 401 (Unauthorized) i czy zapytanie nie było już ponawiane
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                // Pobieramy refreshToken z pamięci
                const currentRefreshToken = await AsyncStorage.getItem('refreshToken');

                if (currentRefreshToken) {
                    // Wysyłamy zapytanie o nowy token używając czystego "axios", aby ominąć interceptory i zapobiec pętli
                    const response = await axios.post('http://10.0.2.2:8080/api/v1/auth/refresh', {
                        refreshToken: currentRefreshToken
                    });

                    const { accessToken, refreshToken } = response.data;

                    // Zapisujemy nowe tokeny w pamięci
                    await AsyncStorage.setItem('accessToken', accessToken);
                    await AsyncStorage.setItem('refreshToken', refreshToken);

                    // Zaktualizuj header z nowym tokenem w oryginalnym, odrzuconym zapytaniu
                    originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;

                    // Ponawiamy oryginalne zapytanie
                    return apiClient(originalRequest);
                }
            } catch (refreshError) {
                // Jeżeli refresh token wygasł lub odświeżenie się nie powiodło:
                // Czyścimy pamięć
                await AsyncStorage.removeItem('accessToken');
                await AsyncStorage.removeItem('refreshToken');

                // Zwracamy błąd (aplikacja powinna to wychwycić i przenieść na ekran logowania)
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);