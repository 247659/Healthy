import {useEffect, useState, useRef} from 'react';
import {Client} from '@stomp/stompjs';
import type {Notification} from "../types/notification.ts";
import {notificationService} from "../api/notificationClient.ts";
import SockJS from 'sockjs-client';

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
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
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
    const [alerts, setAlerts] = useState<Notification[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const clientRef = useRef<Client | null>(null);

    // --- 1. POBIERANIE HISTORYCZNYCH POWIADOMIEŃ PO ZALOGOWANIU ---
    useEffect(() => {
        const fetchInitialNotifications = async () => {
            // Czekamy na poprawny token i ID zalogowanego lekarza (sub)
            const token = localStorage.getItem('access_token');
            if (!token) return;

            const doctorId = getUserIdFromToken(token)

            try {
                // KROK A: Pobieramy pacjentów przypisanych do tego lekarza
                // ⚠️ Dostosuj ten URL do endpointu w swoim serwisie medical-staff!
                const patientsResponse = await fetch(`http://localhost:8080/api/v1/gateway/dashboard/staff/${doctorId}/patients/assigned`, {
                    headers: {Authorization: `Bearer ${token}`}
                });

                if (!patientsResponse.ok) return;
                const patients = await patientsResponse.json();

                // KROK B: Dla każdego pacjenta odpytujemy Twój NotificationController
                const alertPromises = patients.map(async (patient: any) => {

                    const patientId = patient.id || patient.patientId;
                    //TODO W PRZYSZLOSCI WYSYLAC LISTE DLA OGRANICZENIA ZAPYTAN
                    return await notificationService.getNewNotifications(patientId);
                });

                // Czekamy, aż wszystkie zapytania do NotificationController się zakończą
                const allAlertsArrays = await Promise.all(alertPromises);

                // Aktualizujemy stan dzwonka w Navbarze
                const flatAlerts = allAlertsArrays.flat();

                setAlerts(flatAlerts);
                setUnreadCount(flatAlerts.length);

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

        const socketUrl = 'http://localhost:8080/api/v1/ws-notifications';

        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            onConnect: () => {
                console.log('✅ Połączono z WebSocketem powiadomień!');

                client.subscribe('/user/queue/alerts', (message) => {
                    if (message.body) {
                        const newAlert: Notification = JSON.parse(message.body);
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
    }, []);

    const markAllAsRead = () => {
        setUnreadCount(0);
        // Opcjonalnie: Tutaj możesz w przyszłości dodać wywołanie HTTP: 
        // fetch('/api/v1/notifications/mark-read', { method: 'PUT', ... })
        // aby zaktualizować status 'isRead' w bazie danych na true.
    };

    return {alerts, unreadCount, markAllAsRead};
};