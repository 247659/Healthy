import React, { useState, useEffect } from 'react';
import { jwtDecode } from "jwt-decode";
import { StatusBar, StyleSheet, useColorScheme, View, ActivityIndicator } from 'react-native';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { LoginScreen } from './src/screens/LoginScreen';
import { RegisterScreen } from './src/screens/RegisterScreen';
import { ProfileSetupScreen } from './src/screens/ProfileSetupScreen';
import { DashboardScreen } from "./src/screens/DashboardScreen";
import { VitalsHistoryScreen } from './src/screens/VitalsHistoryScreen';
// --- DODANY IMPORT NOWEGO EKRANU ---
import { ProfileEditScreen } from './src/screens/ProfileEditScreen';

import { authService } from './src/api/authClient.ts';

const Stack = createNativeStackNavigator();

function App() {
    const isDarkMode = useColorScheme() === 'dark';
    return (
        <SafeAreaProvider>
            <NavigationContainer>
                <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} backgroundColor="#FFFFFF" />
                <AppContent />
            </NavigationContainer>
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

    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const loadStoredToken = async () => {
            try {
                const storedToken = await AsyncStorage.getItem('access_token');
                const storedRefresh = await AsyncStorage.getItem('refresh_token');
                if (storedToken) {
                    setUserToken(storedToken);
                    setRefreshToken(storedRefresh);
                    await checkProfile(storedToken);
                } else {
                    setIsLoading(false);
                }
            } catch (e) {
                console.error("Błąd odczytu tokenu z AsyncStorage", e);
                setIsLoading(false);
            }
        };
        loadStoredToken();
    }, []);

    const handleLogout = async () => {
        authService.logout(refreshToken);
        await AsyncStorage.removeItem('access_token');
        await AsyncStorage.removeItem('refresh_token');

        setUserToken(null);
        setRefreshToken(null);
        setIsProfileComplete(null);
        setPatientData(null);
        setCurrentScreen('login');
    };

    const checkProfile = async (token: string) => {
        setIsLoading(true);
        try {
            const decoded: any = jwtDecode(token);
            const userId = decoded.sub;

            const response = await fetch(`http://10.0.2.2:8080/api/v1/patients/${userId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) throw new Error("Nie udało się pobrać danych pacjenta.");

            const data = await response.json();
            setPatientData(data);

            const isComplete = Boolean(data && data.dateOfBirth && data.pesel && data.phoneNumber && data.address);
            setIsProfileComplete(isComplete);

        } catch (error) {
            console.error("Błąd podczas dekodowania tokena lub pobierania profilu:", error);
            await handleLogout();
        } finally {
            setIsLoading(false);
        }
    };

    if (isLoading) {
        return (
            <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
                <ActivityIndicator size="large" color="#10B981" />
            </View>
        );
    }

    if (userToken && isProfileComplete === null) {
        return <View style={styles.container} />;
    }

    if (userToken && isProfileComplete === false) {
        return (
            <View style={[styles.container, { paddingTop: safeAreaInsets.top }]}>
                <ProfileSetupScreen
                    patientData={patientData}
                    token={userToken}
                    onProfileUpdated={() => setIsProfileComplete(true)}
                    onLogout={handleLogout}
                />
            </View>
        );
    }

    // --- ZAKTUALIZOWANA NAWIGACJA DLA ZALOGOWANEGO UŻYTKOWNIKA ---
    if (userToken && isProfileComplete === true) {
        return (
            <Stack.Navigator initialRouteName="Dashboard" screenOptions={{ headerShown: false }}>
                <Stack.Screen name="Dashboard">
                    {(props) => (
                        <DashboardScreen
                            {...props}
                            patientData={patientData}
                            token={userToken}
                            onLogout={handleLogout}
                            onNavigateToHistory={(data) =>
                                props.navigation.navigate('VitalsHistory', { patientData: data, token: userToken })
                            }
                            /* --- NOWY PROP DO NAWIGACJI EKRANU EDYCJI PROFILU --- */
                            onNavigateToProfileEdit={(data) =>
                                props.navigation.navigate('ProfileEdit', { patientData: data, token: userToken })
                            }
                        />
                    )}
                </Stack.Screen>

                <Stack.Screen name="VitalsHistory" component={VitalsHistoryScreen} />

                {/* --- DODANY EKRAN EDYCJI PROFILU DO STOSU NAWIGACJI --- */}
                <Stack.Screen name="ProfileEdit" component={ProfileEditScreen} />

            </Stack.Navigator>
        );
    }

    return (
        <View style={[styles.container, { paddingTop: safeAreaInsets.top }]}>
            {currentScreen === 'login' ? (
                <LoginScreen
                    onNavigateToRegister={() => setCurrentScreen('register')}
                    onLoginSuccess={async (tokens) => {
                        await AsyncStorage.setItem('access_token', tokens.accessToken);
                        await AsyncStorage.setItem('refresh_token', tokens.refreshToken);

                        setUserToken(tokens.accessToken);
                        setRefreshToken(tokens.refreshToken);
                        checkProfile(tokens.accessToken);
                    }}
                />
            ) : (
                <RegisterScreen onNavigateToLogin={() => setCurrentScreen('login')} />
            )}
        </View>
    );
}

const styles = StyleSheet.create({ container: { flex: 1, backgroundColor: '#FFFFFF' } });
export default App;