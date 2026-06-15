import React, { useState } from 'react';
import { jwtDecode } from "jwt-decode"; // Import biblioteki
import { StatusBar, StyleSheet, useColorScheme, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native'; // <--- DODANO Text i TouchableOpacity
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';

import { LoginScreen } from './src/screens/LoginScreen';
import { RegisterScreen } from './src/screens/RegisterScreen';
import { ProfileSetupScreen } from './src/screens/ProfileSetupScreen';
import {DashboardScreen} from "./src/screens/DashboardScreen.tsx";

function App() {
    const isDarkMode = useColorScheme() === 'dark';
    return (
        <SafeAreaProvider>
            <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} backgroundColor="#FFFFFF" />
            <AppContent />
        </SafeAreaProvider>
    );
}

function AppContent() {
    const safeAreaInsets = useSafeAreaInsets();
    const [currentScreen, setCurrentScreen] = useState<'login' | 'register'>('login');
    const [userToken, setUserToken] = useState<string | null>(null);
    const [isProfileComplete, setIsProfileComplete] = useState<boolean | null>(null);
    const [patientData, setPatientData] = useState<any>(null);
    const [isLoading, setIsLoading] = useState(false); // <--- DODANO

    const checkProfile = async (token: string) => {
        setIsLoading(true);
        try {
            // 1. Dekodujemy token
            const decoded: any = jwtDecode(token);
            const userId = decoded.sub; // To jest ID pacjenta zakodowane w JWT

            console.log("Zdekodowane ID użytkownika:", userId); // Zmieniłem na log, bo to informacja, nie błąd :)

            // 2. Używamy userId w URL do pobrania danych
            const response = await fetch(`http://10.0.2.2:8088/api/v1/patients/${userId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            const data = await response.json();
            setPatientData(data);

            // 3. Logika sprawdzająca czy profil jest kompletny na podstawie danych
            // Używamy nazewnictwa z Twojego DTO (dateOfBirth, pesel, phoneNumber, address)
            const isComplete = Boolean(
                data &&
                data.dateOfBirth &&
                data.pesel &&
                data.phoneNumber &&
                data.address
            );

            console.log("Czy profil jest kompletny?:", isComplete);

            // Jeśli isComplete to false, to automatycznie przejdzie do ProfileSetupScreen
            setIsProfileComplete(isComplete);

        } catch (error) {
            console.error("Błąd podczas dekodowania tokena lub pobierania profilu:", error);
        } finally {
            setIsLoading(false);
        }
    };

    // EKRAN ŁADOWANIA (Gdy sprawdzamy token)
    if (isLoading) {
        return (
            <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
                <ActivityIndicator size="large" color="#10B981" />
            </View>
        );
    }

    // Jeśli token jest, ale profil wciąż sprawdzany -> nie renderuj nic (lub spinner)
    if (userToken && isProfileComplete === null) {
        return <View style={styles.container} />;
    }

    // 1. Jeśli token jest obecny i profil niepełny
    if (userToken && isProfileComplete === false) {
        return (
            <View style={[styles.container, { paddingTop: safeAreaInsets.top }]}>
                <ProfileSetupScreen
                    patientData={patientData}
                    onProfileUpdated={() => setIsProfileComplete(true)}
                />
            </View>
        );
    }

    // 2. Jeśli profil jest pełny
    if (userToken && isProfileComplete === true) {
        return (
            <DashboardScreen
                patientData={patientData}
                onLogout={() => {
                    setUserToken(null);
                    setIsProfileComplete(null);
                }}
            />
        );
    }

    // 3. Logowanie i Rejestracja
    return (
        <View style={[styles.container, { paddingTop: safeAreaInsets.top }]}>
            {currentScreen === 'login' ? (
                <LoginScreen
                    onNavigateToRegister={() => setCurrentScreen('register')}
                    onLoginSuccess={(token) => {
                        setUserToken(token);
                        checkProfile(token);
                    }}
                />
            ) : (
                <RegisterScreen onNavigateToLogin={() => setCurrentScreen('login')} />
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    successText: {
        fontSize: 18,
        color: '#1F2937',
        marginTop: 10
    }
});

export default App;