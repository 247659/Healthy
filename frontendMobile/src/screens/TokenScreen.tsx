import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Platform } from 'react-native';

interface TokenScreenProps {
    jwt: string;
    onLogout: () => void;
}

export const TokenScreen = ({ jwt, onLogout }: TokenScreenProps) => {
    return (
        <View style={styles.container}>
            <Text style={styles.title}>Witaj w Healthy</Text>
            <Text style={styles.subtitle}>Twój token dostępu:</Text>

            <ScrollView style={styles.tokenBox}>
                <Text style={styles.tokenText}>{jwt}</Text>
            </ScrollView>

            <TouchableOpacity style={styles.logoutButton} onPress={onLogout}>
                <Text style={styles.logoutButtonText}>Wyloguj się</Text>
            </TouchableOpacity>
        </View>
    );
};

const styles = StyleSheet.create({
    container: { flex: 1, padding: 32, justifyContent: 'center', backgroundColor: '#FFFFFF' },
    title: { fontSize: 32, fontWeight: '800', color: '#1F2937', marginBottom: 10 },
    subtitle: { fontSize: 16, color: '#6B7280', marginBottom: 20 },
    tokenBox: { backgroundColor: '#F3F4F6', padding: 15, borderRadius: 12, maxHeight: 200 },
    tokenText: { fontSize: 12, color: '#374151', fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace' },
    logoutButton: { marginTop: 30, backgroundColor: '#EF4444', padding: 16, borderRadius: 12, alignItems: 'center' },
    logoutButtonText: { color: '#FFFFFF', fontWeight: '700', fontSize: 16 }
});