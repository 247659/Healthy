import {useEffect, useState, useRef} from 'react';
import {Client} from '@stomp/stompjs';
import type {Notification} from "../types/notification.ts";
import {notificationService} from "../api/notificationClient.ts";
import SockJS from 'sockjs-client';
import type {Patient} from "../types/patient.ts";
import {useAuthStore} from "../store/auth-store.ts";
import {usePatientStore} from "../store/patients-store.ts";

export const useNotifications = () => {
    const [alerts, setAlerts] = useState<Notification[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const clientRef = useRef<Client | null>(null);
    const user = useAuthStore(state => state.user);
    const assignedPatients = usePatientStore(state => state.assignedPatients)

    // --- 1. POBIERANIE HISTORYCZNYCH POWIADOMIEŃ PO ZALOGOWANIU ---
    useEffect(() => {
        const fetchInitialNotifications = async () => {

            if (!user) return

            try {
                const alertPromises = assignedPatients.map(async (patient: Patient) => {
                    const patientId = patient.id;
                    //TODO W PRZYSZLOSCI WYSYLAC LISTE DLA OGRANICZENIA ZAPYTAN
                    return await notificationService.getNewNotifications(patientId, user.id);
                });
                const allAlertsArrays = await Promise.all(alertPromises);
                const flatAlerts = allAlertsArrays.flat();
                setAlerts(flatAlerts);
                setUnreadCount(flatAlerts.length);

            } catch (error) {
                console.error('Błąd podczas pobierania zaległych powiadomień:', error);
            }
        };

        fetchInitialNotifications();
    }, [assignedPatients, user]);


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

    const markAsRead = async (ids: string[]) => {
        try {
            console.log(ids)
            await notificationService.markAsRead(ids, user?.id || '');
            setUnreadCount((prev) => prev - ids.length);
            setAlerts((prev) => prev.filter(alert => !ids.includes(alert.alertId)));
        } catch (error) {
            console.error('Błąd podczas oznaczania powiadomień jako przeczytane:', error);
        }
    };

    return {alerts, unreadCount, markAsRead};
};