import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView } from 'react-native';
import Svg, { Path, Circle, Polyline, Line, Rect } from 'react-native-svg';

// --- IKONY SVG ---
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

interface DashboardScreenProps {
    patientData: any;
    onLogout: () => void;
}

export const DashboardScreen = ({ patientData, onLogout }: DashboardScreenProps) => {
    // Bezpieczne pobranie imienia (lub domyślnie "Pacjencie")
    const firstName = patientData?.firstName || 'Pacjencie';

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

                {/* KARTA GŁÓWNA (np. nadchodząca wizyta) */}
                <View style={styles.highlightCard}>
                    <View style={styles.cardHeader}>
                        <CalendarIcon color="#FFFFFF" />
                        <Text style={styles.highlightCardTitle}>Najbliższa wizyta</Text>
                    </View>
                    <Text style={styles.highlightCardMainText}>Brak zaplanowanych wizyt</Text>
                    <TouchableOpacity style={styles.highlightButton}>
                        <Text style={styles.highlightButtonText}>Umów wizytę</Text>
                    </TouchableOpacity>
                </View>

                <Text style={styles.sectionTitle}>Szybki dostęp</Text>

                {/* GRID KART */}
                <View style={styles.gridContainer}>
                    <TouchableOpacity style={styles.gridCard}>
                        <View style={styles.iconContainer}>
                            <ActivityIcon />
                        </View>
                        <Text style={styles.gridCardTitle}>Wyniki badań</Text>
                        <Text style={styles.gridCardSubtitle}>Sprawdź historię</Text>
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

    highlightCard: { backgroundColor: '#10B981', borderRadius: 24, padding: 24, marginBottom: 32, shadowColor: '#10B981', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.3, shadowRadius: 12, elevation: 8 },
    cardHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 16, gap: 8 },
    highlightCardTitle: { color: '#D1FAE5', fontSize: 16, fontWeight: '600' },
    highlightCardMainText: { color: '#FFFFFF', fontSize: 22, fontWeight: '700', marginBottom: 20 },
    highlightButton: { backgroundColor: '#FFFFFF', paddingVertical: 12, borderRadius: 12, alignItems: 'center' },
    highlightButtonText: { color: '#10B981', fontSize: 16, fontWeight: '700' },

    sectionTitle: { fontSize: 20, fontWeight: '700', color: '#1F2937', marginBottom: 16 },

    gridContainer: { flexDirection: 'row', gap: 16 },
    gridCard: { flex: 1, backgroundColor: '#FFFFFF', borderRadius: 20, padding: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    iconContainer: { width: 48, height: 48, backgroundColor: '#ECFDF5', borderRadius: 16, justifyContent: 'center', alignItems: 'center', marginBottom: 16 },
    gridCardTitle: { fontSize: 16, fontWeight: '700', color: '#1F2937', marginBottom: 4 },
    gridCardSubtitle: { fontSize: 13, color: '#6B7280' },
});