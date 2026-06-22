import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, ActivityIndicator, Modal, Alert } from 'react-native';
import Svg, { Path, Circle, Polyline, Line, Rect } from 'react-native-svg';
import notifee, { AndroidImportance } from '@notifee/react-native';

// Importujemy nasz klient API
import { dashboardClient } from '../api/dashboardClient';

// --- IKONY ---
const UserIcon = ({ color = "#10B981", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><Circle cx="12" cy="7" r="4" /></Svg>
);
const CalendarIcon = ({ color = "#10B981", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><Line x1="16" y1="2" x2="16" y2="6" /><Line x1="8" y1="2" x2="8" y2="6" /><Line x1="3" y1="10" x2="21" y2="10" /></Svg>
);
const ActivityIcon = ({ color = "#10B981", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Polyline points="22 12 18 12 15 21 9 3 6 12 2 12" /></Svg>
);
const LogoutIcon = ({ color = "#EF4444", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><Polyline points="16 17 21 12 16 7" /><Line x1="21" y1="12" x2="9" y2="12" /></Svg>
);
const ChartIcon = ({ color = "#3B82F6", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M3 3v18h18" /><Path d="M18 9l-5 5-4-4-4 4" /><Circle cx="18" cy="9" r="1" /></Svg>
);
const StethoscopeIcon = ({ color = "#8B5CF6", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3" /><Path d="M8 15v1a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6v-4" /><Circle cx="20" cy="10" r="2" /></Svg>
);
const HeartIcon = ({ color = "#EF4444", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" /></Svg>
);
const ThermometerIcon = ({ color = "#F59E0B", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z" /></Svg>
);
const WindIcon = ({ color = "#3B82F6", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M9.59 4.59A2 2 0 1 1 11 8H2m10.59 11.41A2 2 0 1 0 14 16H2m15.73-8.27A2.5 2.5 0 1 1 19.5 12H2" /></Svg>
);
const BellIcon = ({ color = "#DC2626", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" /><Path d="M13.73 21a2 2 0 0 1-3.46 0" /></Svg>
);
const XIcon = ({ color = "#9ca3af", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><Line x1="18" y1="6" x2="6" y2="18" /><Line x1="6" y1="6" x2="18" y2="18" /></Svg>
);

// Usunięto 'token' z propsów, bo już go nie potrzebujemy ręcznie przekazywać
export const DashboardScreen = ({ patientData, onLogout, onNavigateToHistory, onNavigateToProfileEdit }: any) => {
    const firstName = patientData?.firstName || 'Pacjencie';

    const [latestVitals, setLatestVitals] = useState<any>(null);
    const [isLoadingVitals, setIsLoadingVitals] = useState<boolean>(true);
    const [doctors, setDoctors] = useState<any[]>([]);
    const [isLoadingDoctors, setIsLoadingDoctors] = useState<boolean>(true);
    const [isModalVisible, setIsModalVisible] = useState<boolean>(false);
    const [allDoctors, setAllDoctors] = useState<any[]>([]);
    const [isLoadingAllDoctors, setIsLoadingAllDoctors] = useState<boolean>(false);

    // --- ALERTY ---
    const [alerts, setAlerts] = useState<any[]>([]);
    const [isAlertVisible, setIsAlertVisible] = useState<boolean>(true);
    const lastAlertId = useRef<string | null>(null);
    const isFirstLoad = useRef<boolean>(true);

    useEffect(() => {
        async function requestUserPermission() {
            await notifee.requestPermission();
        }
        requestUserPermission();
    }, []);

    const displayNotification = async (message: string) => {
        const channelId = await notifee.createChannel({
            id: 'health-alerts',
            name: 'Alerty Zdrowotne',
            importance: AndroidImportance.HIGH,
        });

        await notifee.displayNotification({
            title: '🚨 Nowy Alert Zdrowotny',
            body: message,
            android: {
                channelId,
                importance: AndroidImportance.HIGH,
                pressAction: { id: 'default' },
            },
        });
    };

    const fetchVitals = async () => {
        if (!patientData?.id) return;
        try {
            const end = new Date();
            const start = new Date();
            start.setDate(end.getDate() - 2);

            // Wywołujemy klienta bez podawania tokena
            const data = await dashboardClient.getVitals(patientData.id, start.toISOString(), end.toISOString());
            const historyData = Array.isArray(data) ? data : (data.history || []);

            if (historyData.length > 0) {
                const sortedHistory = historyData.sort((a: any, b: any) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
                setLatestVitals(sortedHistory[0].measurements);
            }
        } catch (error) {
            console.error("Błąd pobierania parametrów:", error);
        } finally {
            setIsLoadingVitals(false);
        }
    };

    const fetchDoctors = async () => {
        if (!patientData?.id) return;
        setIsLoadingDoctors(true);
        try {
            // Wywołujemy klienta bez podawania tokena
            const data = await dashboardClient.getAssignedDoctors(patientData.id);
            setDoctors(Array.isArray(data) ? data : []);
        } catch (error) {
            setDoctors([]);
        } finally {
            setIsLoadingDoctors(false);
        }
    };

    const fetchAlerts = async () => {
        if (!patientData?.id) return;
        try {
            // Wywołujemy klienta bez podawania tokena
            const data = await dashboardClient.getAlerts(patientData.id);
            const alertsArray = Array.isArray(data) ? data : [];
            setAlerts(alertsArray);

            if (alertsArray.length > 0) {
                const latestAlert = alertsArray[0];
                const currentLatestId = latestAlert.alertId || latestAlert.id || latestAlert.timestamp;

                // Sprawdzamy czy to całkiem nowy alert
                if (lastAlertId.current !== currentLatestId) {
                    if (!isFirstLoad.current) {
                        const alertMessage = latestAlert.message || "Wykryto nieprawidłowe parametry!";

                        setIsAlertVisible(true);
                        displayNotification(alertMessage);
                    }
                    lastAlertId.current = currentLatestId;
                }
            }
            isFirstLoad.current = false;
        } catch (error) {
            console.error("Błąd pobierania alertów:", error);
        }
    };

    const openAddDoctorModal = async () => {
        setIsModalVisible(true);
        setIsLoadingAllDoctors(true);
        try {
            // Wywołujemy klienta bez podawania tokena
            const data = await dashboardClient.getAllDoctors();
            setAllDoctors(data);
        } catch (error) {
            Alert.alert("Błąd", "Nie udało się załadować listy lekarzy.");
        } finally {
            setIsLoadingAllDoctors(false);
        }
    };

    const handleAssignDoctor = async (doctorId: string) => {
        try {
            // Wywołujemy klienta bez podawania tokena
            await dashboardClient.assignDoctor(doctorId, patientData.id);
            Alert.alert("Sukces", "Lekarz został przypisany!");
            setIsModalVisible(false);
            fetchDoctors();
        } catch (error) {
            Alert.alert("Błąd", "Wystąpił błąd.");
        }
    };

    const handleUnassignDoctor = async (doctorId: string) => {
        try {
            // Wywołujemy klienta bez podawania tokena
            await dashboardClient.unassignDoctor(doctorId, patientData.id);
            Alert.alert("Sukces", "Lekarz został odpięty.");
            fetchDoctors();
        } catch (error) {
            Alert.alert("Błąd", "Wystąpił błąd podczas odpinania lekarza.");
        }
    };

    useEffect(() => {
        fetchVitals();
        fetchDoctors();
        fetchAlerts();

        const intervalId = setInterval(() => {
            fetchVitals();
            fetchAlerts();
        }, 5000);

        return () => clearInterval(intervalId);
        // Z dependencies usunięto token
    }, [patientData?.id]);

    const availableDoctors = allDoctors.filter(doc => !doctors.find(d => d.id === doc.id));

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                <View style={styles.header}>
                    <View>
                        <Text style={styles.greeting}>Cześć, {firstName} 👋</Text>
                        <Text style={styles.subtitle}>Oto Twoje podsumowanie zdrowia</Text>
                    </View>
                    <TouchableOpacity onPress={onLogout} style={styles.logoutButton}>
                        <LogoutIcon size={20} />
                    </TouchableOpacity>
                </View>

                {/* --- ZMODYFIKOWANA SEKCJA ALERTÓW --- */}
                {alerts.length > 0 && isAlertVisible && (
                    <View style={styles.alertBanner}>
                        <View style={styles.alertHeader}>
                            <View style={styles.alertTitleContainer}>
                                <BellIcon size={20} color="#DC2626" />
                                <Text style={styles.alertTitle}>Wykryto anomalię</Text>
                            </View>
                            <TouchableOpacity onPress={() => setIsAlertVisible(false)} hitSlop={{top: 10, bottom: 10, left: 10, right: 10}}>
                                <XIcon size={20} color="#FCA5A5" />
                            </TouchableOpacity>
                        </View>

                        {alerts.slice(0, 1).map((alert, index) => {
                            const dateStr = alert.timestamp ? new Date(alert.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Teraz';
                            return (
                                <View key={index} style={styles.alertContentBox}>
                                    <Text style={styles.alertTime}>{dateStr}</Text>
                                    <Text style={styles.alertMessage}>{alert.message || 'Zarejestrowano nieprawidłowe parametry życiowe.'}</Text>
                                </View>
                            );
                        })}
                    </View>
                )}

                {/* --- BIEŻĄCE PARAMETRY --- */}
                <Text style={styles.sectionTitle}>Bieżące parametry</Text>
                {isLoadingVitals && !latestVitals ? (
                    <ActivityIndicator size="large" color="#10B981" style={{ marginVertical: 20 }} />
                ) : latestVitals ? (
                    <View style={styles.vitalsGrid}>
                        <View style={styles.vitalCard}>
                            <HeartIcon size={28} color="#EF4444" />
                            <Text style={styles.vitalValue}>{latestVitals.heartRate ?? '--'} <Text style={styles.vitalUnit}>bpm</Text></Text>
                            <Text style={styles.vitalLabel}>Tętno</Text>
                        </View>
                        <View style={styles.vitalCard}>
                            <ActivityIcon size={28} color="#8B5CF6" />
                            <Text style={styles.vitalValue}>
                                {latestVitals.bloodPressure?.systolic ?? '--'}/{latestVitals.bloodPressure?.diastolic ?? '--'}
                            </Text>
                            <Text style={styles.vitalLabel}>Ciśnienie</Text>
                        </View>
                        <View style={styles.vitalCard}>
                            <ThermometerIcon size={28} color="#F59E0B" />
                            <Text style={styles.vitalValue}>{latestVitals.temperature ?? '--'}°C</Text>
                            <Text style={styles.vitalLabel}>Temperatura</Text>
                        </View>
                        <View style={styles.vitalCard}>
                            <WindIcon size={28} color="#3B82F6" />
                            <Text style={styles.vitalValue}>{latestVitals.spO2 ?? '--'}%</Text>
                            <Text style={styles.vitalLabel}>SpO2</Text>
                        </View>
                    </View>
                ) : (
                    <View style={styles.noDataCard}>
                        <Text style={styles.noDataText}>Brak danych z czujnika.</Text>
                    </View>
                )}

                <TouchableOpacity style={styles.historyButton} onPress={() => onNavigateToHistory(patientData)}>
                    <View style={styles.historyIconWrapper}><ChartIcon size={22} color="#FFFFFF" /></View>
                    <View style={styles.historyTextWrapper}>
                        <Text style={styles.historyTitle}>Historia i trendy</Text>
                        <Text style={styles.historySubtitle}>Zobacz wykresy z ostatnich dni</Text>
                    </View>
                    <Text style={styles.historyChevron}>›</Text>
                </TouchableOpacity>

                <Text style={[styles.sectionTitle, { marginTop: 24 }]}>Szybki dostęp</Text>
                <View style={styles.gridContainer}>
                    <TouchableOpacity style={styles.gridCard}>
                        <View style={styles.iconContainer}><CalendarIcon /></View>
                        <Text style={styles.gridCardTitle}>Wizyty</Text>
                        <Text style={styles.gridCardSubtitle}>Umów lub sprawdź</Text>
                    </TouchableOpacity>
                    <TouchableOpacity style={styles.gridCard} onPress={() => onNavigateToProfileEdit(patientData)}>
                        <View style={styles.iconContainer}><UserIcon /></View>
                        <Text style={styles.gridCardTitle}>Mój Profil</Text>
                        <Text style={styles.gridCardSubtitle}>Zarządzaj danymi</Text>
                    </TouchableOpacity>
                </View>

                {/* --- PRZYPISANI LEKARZE --- */}
                <View style={styles.sectionHeaderRow}>
                    <Text style={styles.sectionTitleNoMargin}>Przypisani lekarze</Text>
                    <TouchableOpacity style={styles.addButton} onPress={openAddDoctorModal}>
                        <Text style={styles.addButtonText}>+ Dodaj</Text>
                    </TouchableOpacity>
                </View>

                <View style={styles.doctorsContainer}>
                    {isLoadingDoctors ? (
                        <ActivityIndicator size="large" color="#8B5CF6" style={{ marginVertical: 20 }} />
                    ) : doctors.length > 0 ? (
                        doctors.map((doctor, index) => {
                            const specName = doctor.specializationNames && doctor.specializationNames.length > 0 ? doctor.specializationNames[0] : 'Lekarz specjalista';
                            return (
                                <View key={doctor.id || index} style={styles.doctorCard}>
                                    <View style={[styles.doctorAvatar, { backgroundColor: index % 2 === 0 ? "#F3E8FF" : "#EFF6FF" }]}>
                                        <StethoscopeIcon color={index % 2 === 0 ? "#8B5CF6" : "#3B82F6"} size={24} />
                                    </View>
                                    <View style={styles.doctorInfo}>
                                        <Text style={styles.doctorName}>Dr {doctor.firstName} {doctor.lastName}</Text>
                                        <Text style={styles.doctorSpecialty}>{specName}</Text>
                                    </View>
                                    <TouchableOpacity
                                        style={styles.unassignButton}
                                        onPress={() => Alert.alert("Potwierdź", "Czy na pewno chcesz odpiąć tego lekarza?", [
                                            { text: "Anuluj", style: "cancel" },
                                            { text: "Tak", onPress: () => handleUnassignDoctor(doctor.id) }
                                        ])}
                                    >
                                        <Text style={styles.unassignButtonText}>Odepnij</Text>
                                    </TouchableOpacity>
                                </View>
                            );
                        })
                    ) : (
                        <View style={styles.noDataCard}>
                            <Text style={styles.noDataText}>Obecnie nie masz przypisanych lekarzy.</Text>
                        </View>
                    )}
                </View>

            </ScrollView>

            {/* --- MODAL --- */}
            <Modal visible={isModalVisible} transparent={true} animationType="slide" onRequestClose={() => setIsModalVisible(false)}>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>Wybierz lekarza</Text>
                        {isLoadingAllDoctors ? (
                            <ActivityIndicator size="large" color="#10B981" style={{ marginVertical: 40 }} />
                        ) : availableDoctors.length > 0 ? (
                            <ScrollView style={styles.modalScroll} showsVerticalScrollIndicator={false}>
                                {availableDoctors.map((doc, i) => (
                                    <View key={doc.id || i} style={styles.modalDoctorCard}>
                                        <View style={styles.modalDoctorInfo}>
                                            <Text style={styles.modalDoctorName}>Dr {doc.firstName} {doc.lastName}</Text>
                                            <Text style={styles.modalDoctorSpecialty}>{doc.specializationNames?.[0] || 'Lekarz'}</Text>
                                        </View>
                                        <TouchableOpacity style={styles.modalAssignBtn} onPress={() => handleAssignDoctor(doc.id)}>
                                            <Text style={styles.modalAssignBtnText}>Dodaj</Text>
                                        </TouchableOpacity>
                                    </View>
                                ))}
                            </ScrollView>
                        ) : (
                            <View style={{ padding: 20, alignItems: 'center' }}><Text style={styles.noDataText}>Brak lekarzy.</Text></View>
                        )}
                        <TouchableOpacity onPress={() => setIsModalVisible(false)} style={styles.modalCloseBtn}>
                            <Text style={styles.modalCloseBtnText}>Zamknij</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FAFAFA' },
    container: { padding: 24, paddingBottom: 40 },

    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, marginTop: 16 },
    greeting: { fontSize: 28, fontWeight: '800', color: '#1F2937' },
    subtitle: { fontSize: 15, color: '#6B7280', marginTop: 4 },
    logoutButton: { padding: 10, backgroundColor: '#FEE2E2', borderRadius: 12 },

    alertBanner: {
        backgroundColor: '#FEF2F2',
        borderRadius: 16,
        marginBottom: 24,
        borderLeftWidth: 6,
        borderLeftColor: '#EF4444',
        shadowColor: '#EF4444',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 10,
        elevation: 3,
        padding: 16
    },
    alertHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
    alertTitleContainer: { flexDirection: 'row', alignItems: 'center' },
    alertTitle: { fontSize: 16, fontWeight: '800', color: '#991B1B', marginLeft: 8 },
    alertContentBox: { backgroundColor: '#FFFFFF', padding: 12, borderRadius: 12, flexDirection: 'row', alignItems: 'center' },
    alertTime: { fontSize: 13, fontWeight: '800', color: '#DC2626', marginRight: 8 },
    alertMessage: { flex: 1, fontSize: 13, color: '#4B5563', lineHeight: 18 },

    sectionTitle: { fontSize: 20, fontWeight: '700', color: '#1F2937', marginBottom: 16 },
    sectionHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 24, marginBottom: 16 },
    sectionTitleNoMargin: { fontSize: 20, fontWeight: '700', color: '#1F2937' },

    addButton: { backgroundColor: '#ECFDF5', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12 },
    addButtonText: { color: '#10B981', fontWeight: '700', fontSize: 14 },

    vitalsGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 16, justifyContent: 'space-between' },
    vitalCard: { backgroundColor: '#FFFFFF', width: '47%', borderRadius: 20, padding: 16, alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    vitalValue: { fontSize: 22, fontWeight: '800', color: '#1F2937', marginTop: 8 },
    vitalUnit: { fontSize: 14, fontWeight: '600', color: '#9CA3AF' },
    vitalLabel: { fontSize: 13, color: '#6B7280', marginTop: 4, fontWeight: '500' },

    noDataCard: { backgroundColor: '#FFFFFF', padding: 20, borderRadius: 16, alignItems: 'center' },
    noDataText: { color: '#6B7280', fontSize: 14, textAlign: 'center' },

    historyButton: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', marginTop: 20, padding: 16, borderRadius: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    historyIconWrapper: { width: 44, height: 44, borderRadius: 12, backgroundColor: '#3B82F6', justifyContent: 'center', alignItems: 'center', marginRight: 16 },
    historyTextWrapper: { flex: 1 },
    historyTitle: { fontSize: 16, fontWeight: '700', color: '#1F2937' },
    historySubtitle: { fontSize: 13, color: '#6B7280', marginTop: 2 },
    historyChevron: { fontSize: 24, color: '#9CA3AF', paddingHorizontal: 8 },

    gridContainer: { flexDirection: 'row', gap: 16 },
    gridCard: { flex: 1, backgroundColor: '#FFFFFF', borderRadius: 20, padding: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    iconContainer: { width: 48, height: 48, backgroundColor: '#ECFDF5', borderRadius: 16, justifyContent: 'center', alignItems: 'center', marginBottom: 16 },
    gridCardTitle: { fontSize: 16, fontWeight: '700', color: '#1F2937', marginBottom: 4 },
    gridCardSubtitle: { fontSize: 13, color: '#6B7280' },

    doctorsContainer: { gap: 12 },
    doctorCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', padding: 16, borderRadius: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    doctorAvatar: { width: 48, height: 48, borderRadius: 24, justifyContent: 'center', alignItems: 'center', marginRight: 16 },
    doctorInfo: { flex: 1 },
    doctorName: { fontSize: 16, fontWeight: '700', color: '#1F2937' },
    doctorSpecialty: { fontSize: 13, color: '#6B7280', marginTop: 2 },
    unassignButton: { backgroundColor: '#FEE2E2', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12 },
    unassignButtonText: { color: '#EF4444', fontWeight: '700', fontSize: 12 },

    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center', padding: 24 },
    modalContent: { width: '100%', maxHeight: '80%', backgroundColor: '#FFFFFF', borderRadius: 24, padding: 24 },
    modalTitle: { fontSize: 22, fontWeight: '800', color: '#1F2937', marginBottom: 16, textAlign: 'center' },
    modalScroll: { marginBottom: 16 },
    modalDoctorCard: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
    modalDoctorInfo: { flex: 1, paddingRight: 12 },
    modalDoctorName: { fontSize: 16, fontWeight: '700', color: '#1F2937' },
    modalDoctorSpecialty: { fontSize: 13, color: '#6B7280', marginTop: 2 },
    modalAssignBtn: { backgroundColor: '#10B981', paddingHorizontal: 16, paddingVertical: 8, borderRadius: 12 },
    modalAssignBtnText: { color: '#FFFFFF', fontWeight: '700', fontSize: 13 },
    modalCloseBtn: { backgroundColor: '#F3F4F6', paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
    modalCloseBtnText: { color: '#4B5563', fontSize: 16, fontWeight: '700' }
});