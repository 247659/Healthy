import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, ActivityIndicator } from 'react-native';
import Svg, { Path, Circle, Polyline, Line, Rect } from 'react-native-svg';

// --- IKONY GŁÓWNE ---
const UserIcon = ({ color = "#10B981", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <Circle cx="12" cy="7" r="4" />
    </Svg>
);

const CalendarIcon = ({ color = "#10B981", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
        <Line x1="16" y1="2" x2="16" y2="6" />
        <Line x1="8" y1="2" x2="8" y2="6" />
        <Line x1="3" y1="10" x2="21" y2="10" />
    </Svg>
);

const ActivityIcon = ({ color = "#10B981", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
    </Svg>
);

const LogoutIcon = ({ color = "#EF4444", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <Polyline points="16 17 21 12 16 7" />
        <Line x1="21" y1="12" x2="9" y2="12" />
    </Svg>
);

// --- IKONY PARAMETRÓW ŻYCIOWYCH ---
const HeartIcon = ({ color = "#EF4444", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
    </Svg>
);

const ThermometerIcon = ({ color = "#F59E0B", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z" />
    </Svg>
);

const WindIcon = ({ color = "#3B82F6", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M9.59 4.59A2 2 0 1 1 11 8H2m10.59 11.41A2 2 0 1 0 14 16H2m15.73-8.27A2.5 2.5 0 1 1 19.5 12H2" />
    </Svg>
);

interface DashboardScreenProps {
    patientData: any;
    onLogout: () => void;
}

export const DashboardScreen = ({ patientData, onLogout }: DashboardScreenProps) => {
    const firstName = patientData?.firstName || 'Pacjencie';

    // Stany dla danych z czujników
    const [latestVitals, setLatestVitals] = useState<any>(null);
    const [isLoadingVitals, setIsLoadingVitals] = useState<boolean>(true);

    const fetchVitals = async () => {
        // Zabezpieczenie przed brakiem ID pacjenta
        if (!patientData?.id) return;

        try {
            // Generujemy dynamiczne daty, żeby zawsze pobierać np. z ostatnich 2 dni
            const end = new Date();
            const start = new Date();
            start.setDate(end.getDate() - 2);

            const startTime = start.toISOString();
            const endTime = end.toISOString();

            // Pamiętaj: Jeśli używasz emulatora Androida, zamiast localhost użyj 10.0.2.2
            const API_URL = `http://10.0.2.2:8080/api/vitals/${patientData.id}?start_time=${startTime}&end_time=${endTime}`;

            const response = await fetch(API_URL);
            const data = await response.json();

            // Jeśli zwrócono historię z elementami
            if (data.history && data.history.length > 0) {
                // Sortujemy malejąco po timestamp, żeby pierwszy element był najnowszy
                const sortedHistory = data.history.sort((a: any, b: any) =>
                    new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
                );

                setLatestVitals(sortedHistory[0].measurements);
            }
        } catch (error) {
            console.error("Błąd podczas pobierania parametrów życiowych:", error);
        } finally {
            setIsLoadingVitals(false);
        }
    };

    useEffect(() => {
        // Pobierz natychmiast przy montowaniu komponentu
        fetchVitals();

        // Ustaw interwał na 30 sekund (30 000 ms)
        const intervalId = setInterval(() => {
            fetchVitals();
        }, 30000);

        // Czyszczenie interwału przy odmontowaniu komponentu (zapobiega wyciekom pamięci)
        return () => clearInterval(intervalId);
    }, [patientData?.id]);

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView contentContainerStyle={styles.container}>

                {/* HEADER */}
                <View style={styles.header}>
                    <View>
                        <Text style={styles.greeting}>Cześć, {firstName} 👋</Text>
                        <Text style={styles.subtitle}>Oto Twoje podsumowanie zdrowia</Text>
                    </View>
                    <TouchableOpacity onPress={onLogout} style={styles.logoutButton}>
                        <LogoutIcon size={20} />
                    </TouchableOpacity>
                </View>

                {/* AKTUALNE PARAMETRY Z CZUJNIKA */}
                <Text style={styles.sectionTitle}>Bieżące parametry</Text>

                {isLoadingVitals && !latestVitals ? (
                    <ActivityIndicator size="large" color="#10B981" style={{ marginVertical: 20 }} />
                ) : latestVitals ? (
                    <View style={styles.vitalsGrid}>
                        {/* TĘTNO */}
                        <View style={styles.vitalCard}>
                            <HeartIcon size={28} color="#EF4444" />
                            <Text style={styles.vitalValue}>{latestVitals.heartRate} <Text style={styles.vitalUnit}>bpm</Text></Text>
                            <Text style={styles.vitalLabel}>Tętno</Text>
                        </View>

                        {/* CIŚNIENIE */}
                        <View style={styles.vitalCard}>
                            <ActivityIcon size={28} color="#8B5CF6" />
                            <Text style={styles.vitalValue}>
                                {latestVitals.bloodPressure.systolic}/{latestVitals.bloodPressure.diastolic}
                            </Text>
                            <Text style={styles.vitalLabel}>Ciśnienie krwi</Text>
                        </View>

                        {/* TEMPERATURA */}
                        <View style={styles.vitalCard}>
                            <ThermometerIcon size={28} color="#F59E0B" />
                            <Text style={styles.vitalValue}>{latestVitals.temperature}°C</Text>
                            <Text style={styles.vitalLabel}>Temperatura</Text>
                        </View>

                        {/* SATURACJA */}
                        <View style={styles.vitalCard}>
                            <WindIcon size={28} color="#3B82F6" />
                            <Text style={styles.vitalValue}>{latestVitals.spO2}%</Text>
                            <Text style={styles.vitalLabel}>SpO2</Text>
                        </View>
                    </View>
                ) : (
                    <View style={styles.noDataCard}>
                        <Text style={styles.noDataText}>Brak danych z czujnika.</Text>
                    </View>
                )}

                <Text style={[styles.sectionTitle, { marginTop: 24 }]}>Szybki dostęp</Text>

                {/* GRID KART (Menu) */}
                <View style={styles.gridContainer}>
                    <TouchableOpacity style={styles.gridCard}>
                        <View style={styles.iconContainer}>
                            <CalendarIcon />
                        </View>
                        <Text style={styles.gridCardTitle}>Wizyty</Text>
                        <Text style={styles.gridCardSubtitle}>Umów lub sprawdź</Text>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.gridCard}>
                        <View style={styles.iconContainer}>
                            <UserIcon />
                        </View>
                        <Text style={styles.gridCardTitle}>Mój Profil</Text>
                        <Text style={styles.gridCardSubtitle}>Zarządzaj danymi</Text>
                    </TouchableOpacity>
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FAFAFA' },
    container: { padding: 24, paddingBottom: 40 },

    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32, marginTop: 16 },
    greeting: { fontSize: 28, fontWeight: '800', color: '#1F2937' },
    subtitle: { fontSize: 15, color: '#6B7280', marginTop: 4 },
    logoutButton: { padding: 10, backgroundColor: '#FEE2E2', borderRadius: 12 },

    sectionTitle: { fontSize: 20, fontWeight: '700', color: '#1F2937', marginBottom: 16 },

    // --- Sekcja Vitals (Pomiary) ---
    vitalsGrid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 16,
        justifyContent: 'space-between'
    },
    vitalCard: {
        backgroundColor: '#FFFFFF',
        width: '47%',
        borderRadius: 20,
        padding: 16,
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 8,
        elevation: 2,
    },
    vitalValue: {
        fontSize: 22,
        fontWeight: '800',
        color: '#1F2937',
        marginTop: 8,
    },
    vitalUnit: {
        fontSize: 14,
        fontWeight: '600',
        color: '#9CA3AF',
    },
    vitalLabel: {
        fontSize: 13,
        color: '#6B7280',
        marginTop: 4,
        fontWeight: '500'
    },
    noDataCard: {
        backgroundColor: '#FFFFFF',
        padding: 20,
        borderRadius: 16,
        alignItems: 'center'
    },
    noDataText: {
        color: '#6B7280',
        fontSize: 14,
    },

    // --- Sekcja Menu ---
    gridContainer: { flexDirection: 'row', gap: 16 },
    gridCard: { flex: 1, backgroundColor: '#FFFFFF', borderRadius: 20, padding: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    iconContainer: { width: 48, height: 48, backgroundColor: '#ECFDF5', borderRadius: 16, justifyContent: 'center', alignItems: 'center', marginBottom: 16 },
    gridCardTitle: { fontSize: 16, fontWeight: '700', color: '#1F2937', marginBottom: 4 },
    gridCardSubtitle: { fontSize: 13, color: '#6B7280' },
});