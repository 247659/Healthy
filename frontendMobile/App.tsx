import React, { useState } from 'react';
import { jwtDecode } from "jwt-decode";
import { StatusBar, StyleSheet, useColorScheme, View, ActivityIndicator } from 'react-native';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';

import { LoginScreen } from './src/screens/LoginScreen';
import { RegisterScreen } from './src/screens/RegisterScreen';
import { ProfileSetupScreen } from './src/screens/ProfileSetupScreen';
import { DashboardScreen } from "./src/screens/DashboardScreen";
import { authService } from './src/api/authClient.ts';

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
    const [refreshToken, setRefreshToken] = useState<string | null>(null);
    const [isProfileComplete, setIsProfileComplete] = useState<boolean | null>(null);
    const [patientData, setPatientData] = useState<any>(null);
    const [isLoading, setIsLoading] = useState(false);

    // --- FUNKCJA WYLOGOWANIA ---
    const handleLogout = () => {
        authService.logout(refreshToken);
        setUserToken(null);
        setRefreshToken(null);
        setIsProfileComplete(null);
        setPatientData(null); // Czyścimy dane pacjenta przy wylogowaniu
    };

    const checkProfile = async (token: string) => {
        setIsLoading(true);
        try {
            const decoded: any = jwtDecode(token);
            const userId = decoded.sub;

            console.log("Zdekodowane ID użytkownika:", userId);

            const response = await fetch(`http://10.0.2.2:8080/api/v1/patients/${userId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) throw new Error("Nie udało się pobrać danych pacjenta.");

            const data = await response.json();
            setPatientData(data);

            const isComplete = Boolean(
                data &&
                data.dateOfBirth &&
                data.pesel &&
                data.phoneNumber &&
                data.address
            );

            console.log("Czy profil jest kompletny?:", isComplete);

            setIsProfileComplete(isComplete);

        } catch (error) {
            console.error("Błąd podczas dekodowania tokena lub pobierania profilu:", error);
            // W razie błędu pobierania (np. token wygasł), bezpiecznie wylogowujemy
            handleLogout();
        } finally {
            setIsLoading(false);
        }
    };

    // EKRAN ŁADOWANIA
    if (isLoading) {
        return (
            <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
                <ActivityIndicator size="large" color="#10B981" />
            </View>
        );
    }

    // Jeśli token jest, ale profil wciąż sprawdzany
    if (userToken && isProfileComplete === null) {
        return <View style={styles.container} />;
    }

    // 1. Jeśli token jest obecny i profil NIEPEŁNY
    if (userToken && isProfileComplete === false) {
        return (
            <View style={[styles.container, { paddingTop: safeAreaInsets.top }]}>
                <ProfileSetupScreen
                    patientData={patientData}
                    onProfileUpdated={() => setIsProfileComplete(true)}
                    onLogout={handleLogout} // <--- DODANO BRAKUJĄCY PROP
                />
            </View>
        );
    }

    // 2. Jeśli token jest obecny i profil JEST PEŁNY
    if (userToken && isProfileComplete === true) {
        return (
            <DashboardScreen
                patientData={patientData}
                onLogout={handleLogout} // <--- Używamy wspólnej funkcji
            />
        );
    }

    // 3. Logowanie i Rejestracja (Brak tokena)
    return (
        <View style={[styles.container, { paddingTop: safeAreaInsets.top }]}>
            {currentScreen === 'login' ? (
                <LoginScreen
                    onNavigateToRegister={() => setCurrentScreen('register')}
                    onLoginSuccess={(token) => {
                        setUserToken(token.accessToken);
                        checkProfile(token.accessToken);
                        setRefreshToken(token.refreshToken);
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
});

export default App;