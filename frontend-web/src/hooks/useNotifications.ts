import { useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import keycloak from '../keycloak';

export interface AlertDto {
    alertId?: number;
    patientId: string;
    riskScore: number;
    message: string;
    timestamp: string;
    isRead?: boolean;
}

const getUserIdFromToken = (token: string) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        const decoded = JSON.parse(jsonPayload);
        return decoded.sub; // W Keycloak pole 'sub' to UUID użytkownika
    } catch (e) {
        console.error("Błąd dekodowania tokenu", e);
        return null;
    }
};

export const useNotifications = () => {
    const [alerts, setAlerts] = useState<AlertDto[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const clientRef = useRef<Client | null>(null);

    // --- 1. POBIERANIE HISTORYCZNYCH POWIADOMIEŃ PO ZALOGOWANIU ---
    useEffect(() => {
        const fetchInitialNotifications = async () => {
            // Czekamy na poprawny token i ID zalogowanego lekarza (sub)
            const token = localStorage.getItem('access_token');

            const doctorId = getUserIdFromToken(token)

            try {
                // KROK A: Pobieramy pacjentów przypisanych do tego lekarza
                // ⚠️ Dostosuj ten URL do endpointu w swoim serwisie medical-staff!
                const patientsResponse = await fetch(`http://localhost:8080/api/v1/gateway/dashboard/staff/${doctorId}/patients/assigned`, {
                    headers: { Authorization: `Bearer ${token}` }
                });

                if (!patientsResponse.ok) return;
                const patients = await patientsResponse.json();

                // KROK B: Dla każdego pacjenta odpytujemy Twój NotificationController
                const alertPromises = patients.map(async (patient: any) => {
                    // Zakładam, że encja pacjenta ma pole 'id' lub 'patientId'
                    const patientId = patient.id || patient.patientId;

                    //TODO ZMIENIC TO NA PRZESLANIE LISTY IDS
                    const res = await fetch(`http://localhost:8080/api/v1/notifications/${patientId}`, {
                        headers: { Authorization: `Bearer ${token}` }
                    });

                    if (res.ok) {
                        return await res.json();
                    }
                    return [];
                });

                // Czekamy, aż wszystkie zapytania do NotificationController się zakończą
                const allAlertsArrays = await Promise.all(alertPromises);

                // KROK C: Spłaszczamy tablicę z tablicami, filtrujemy i sortujemy po dacie (najnowsze na górze)
                const combinedAlerts = allAlertsArrays
                    .flat()
                    .filter((alert: AlertDto) => alert.isRead === false || alert.isRead === undefined) // Zostawiamy tylko nieodczytane
                    .sort((a: AlertDto, b: AlertDto) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());

                // Aktualizujemy stan dzwonka w Navbarze
                setAlerts(combinedAlerts);
                setUnreadCount(combinedAlerts.length);

            } catch (error) {
                console.error('Błąd podczas pobierania zaległych powiadomień:', error);
            }
        };

        fetchInitialNotifications();
    }, []);


    // --- 2. NASŁUCHIWANIE NA NOWE POWIADOMIENIA (WEBSOCKET) ---
    useEffect(() => {
        const token = localStorage.getItem('access_token');
        if (!token) return;

        const socketUrl = 'ws://localhost:8080/api/v1/ws-notifications';

        const client = new Client({
            brokerURL: socketUrl,
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            onConnect: () => {
                console.log('✅ Połączono z WebSocketem powiadomień!');

                client.subscribe('/user/queue/alerts', (message) => {
                    if (message.body) {
                        const newAlert: AlertDto = JSON.parse(message.body);
                        console.log('🚨 Nowy Alert z RabbitMQ:', newAlert);

                        // Dodajemy nowe powiadomienie na początek listy
                        setAlerts((prevAlerts) => [newAlert, ...prevAlerts]);
                        setUnreadCount((prevCount) => prevCount + 1);
                    }
                });
            },
            onStompError: (frame) => {
                console.error('Błąd brokera: ' + frame.headers['message']);
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, [keycloak.authenticated, keycloak.token]);

    const markAllAsRead = () => {
        setUnreadCount(0);
        // Opcjonalnie: Tutaj możesz w przyszłości dodać wywołanie HTTP: 
        // fetch('/api/v1/notifications/mark-read', { method: 'PUT', ... })
        // aby zaktualizować status 'isRead' w bazie danych na true.
    };

    return { alerts, unreadCount, markAllAsRead };
};