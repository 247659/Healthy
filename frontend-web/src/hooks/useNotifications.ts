import { useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import keycloak from '../keycloak';

export interface AlertDto {
    patientId: string;
    riskScore: number;
    message: string;
    timestamp: string;
}

export const useNotifications = () => {
    const [alerts, setAlerts] = useState<AlertDto[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    // const [token, setToken] = useState<string>();
    const clientRef = useRef<Client | null>(null);

    useEffect(() => {
        // Czekamy aż Keycloak się załaduje i użytkownik będzie zalogowany
        const token = localStorage.getItem('access_token');
        if (!token) return;

        // Adres musi wskazywać na API Gateway lub bezpośrednio na Notification Service (port 8085)
        const socketUrl = 'http://localhost:8080/api/v1/ws-notifications';

        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            // Przekazujemy token JWT w nagłówku CONNECT
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            debug: (str) => {
                // Odkomentuj poniższą linię, aby widzieć logi WebSocket w konsoli przeglądarki
                console.log(str); 
            },
            onConnect: () => {
                console.log('✅ Połączono z WebSocketem powiadomień!');

                // Subskrybujemy kanał prywatny dla zalogowanego lekarza
                client.subscribe('/user/queue/alerts', (message) => {
                    if (message.body) {
                        const newAlert: AlertDto = JSON.parse(message.body);
                        console.log('🚨 Nowy Alert:', newAlert);

                        // Dodajemy nowy alert na początek listy
                        setAlerts((prevAlerts) => [newAlert, ...prevAlerts]);
                        setUnreadCount((prevCount) => prevCount + 1);

                        // Opcjonalnie: Systemowe powiadomienie dźwiękowe
                        // new Audio('/alert-sound.mp3').play().catch(e => console.log(e));
                    }
                });
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
            }
        });

        client.activate();
        clientRef.current = client;

        // Cleanup: rozłączenie po wylogowaniu lub zamknięciu komponentu
        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, [keycloak.authenticated, keycloak.token]);

    // Funkcja czyszcząca licznik, gdy lekarz kliknie w dzwonek
    const markAllAsRead = () => {
        setUnreadCount(0);
    };

    return { alerts, unreadCount, markAllAsRead };
};