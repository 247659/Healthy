import React, { useState } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
    ActivityIndicator,
    KeyboardAvoidingView,
    Platform,
} from 'react-native';
import Svg, { Path, Circle, Line } from 'react-native-svg';
import AsyncStorage from '@react-native-async-storage/async-storage'; // Dodany import
import { authService } from '../api/authClient.ts';
import { LoginRequest, Token } from '../types/auth.ts';
import axios from 'axios';

// --- IKONY ---
interface IconProps { color?: string; size?: number; }
const EyeIcon = ({ color = "#9CA3AF", size = 24 }: IconProps) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
        <Circle cx="12" cy="12" r="3" />
    </Svg>
);
const EyeOffIcon = ({ color = "#9CA3AF", size = 24 }: IconProps) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
        <Line x1="1" y1="1" x2="23" y2="23" />
    </Svg>
);

interface LoginScreenProps {
    onNavigateToRegister: () => void;
    onLoginSuccess: (token: Token) => void;
}

export const LoginScreen = ({ onNavigateToRegister, onLoginSuccess }: LoginScreenProps) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const handleLogin = async () => {
        setErrorMessage(null);
        if (!email || !password) {
            setErrorMessage('Wypełnij wszystkie pola formularza.');
            return;
        }
        setIsLoading(true);
        try {
            const loginData: LoginRequest = {
                email: email,
                password: password,
            };

            // Wysyłamy żądanie do API
            const response = await authService.login(loginData);

            // ZAPIS DO PAMIĘCI URZĄDZENIA - To jest brakujący krok!
            // Teraz interceptor z apiClient będzie miał skąd pobrać token.
            await AsyncStorage.setItem('accessToken', response.accessToken);
            await AsyncStorage.setItem('refreshToken', response.refreshToken);

            // Przekazujemy dalej
            onLoginSuccess(response);

        } catch (error: any) {
            if (axios.isAxiosError(error)) {
                if (error.response?.status === 401) {
                    setErrorMessage("Nieprawidłowy email lub hasło");
                } else {
                    setErrorMessage("Błąd podczas logowania. Spróbuj ponownie później.");
                }
            } else {
                setErrorMessage('Brak połączenia z serwerem.');
            }

        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
            <View style={styles.formContainer}>
                <View style={styles.headerContainer}>
                    <Text style={styles.brandName}>Healthy<Text style={styles.brandDot}>.</Text></Text>
                    <Text style={styles.subtitle}>Witaj ponownie! Zaloguj się, aby kontynuować.</Text>
                </View>

                {errorMessage ? (
                    <View style={styles.errorContainer}>
                        <Text style={styles.errorText}>{errorMessage}</Text>
                    </View>
                ) : null}

                <View style={styles.inputContainer}>
                    {/* Grupa Adres e-mail */}
                    <View>
                        <Text style={styles.inputLabel}>Adres e-mail</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="Wpisz swój e-mail"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="email-address"
                            autoCapitalize="none"
                            value={email}
                            onChangeText={setEmail}
                            editable={!isLoading}
                        />
                    </View>

                    {/* Grupa Hasło */}
                    <View>
                        <Text style={styles.inputLabel}>Hasło</Text>
                        {/* Pole hasła z ikoną oczka */}
                        <View style={styles.passwordContainer}>
                            <TextInput
                                style={styles.passwordInput}
                                placeholder="Wpisz swoje hasło"
                                placeholderTextColor="#9CA3AF"
                                secureTextEntry={!showPassword}
                                value={password}
                                onChangeText={setPassword}
                                editable={!isLoading}
                            />
                            <TouchableOpacity
                                style={styles.eyeIcon}
                                onPress={() => setShowPassword(!showPassword)}
                                hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                            >
                                {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>

                <TouchableOpacity style={styles.forgotPasswordButton}>
                    <Text style={styles.forgotPasswordText}>Zapomniałeś hasła?</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.primaryButton} onPress={handleLogin} disabled={isLoading}>
                    {isLoading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryButtonText}>Zaloguj się</Text>}
                </TouchableOpacity>

                <View style={styles.footerContainer}>
                    <Text style={styles.footerText}>Nie masz jeszcze konta? </Text>
                    <TouchableOpacity onPress={onNavigateToRegister} disabled={isLoading}>
                        <Text style={styles.footerLink}>Zarejestruj się</Text>
                    </TouchableOpacity>
                </View>
            </View>
        </KeyboardAvoidingView>
    );
};

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FFFFFF' },
    formContainer: { flex: 1, justifyContent: 'center', paddingHorizontal: 32 },
    headerContainer: { marginBottom: 32, alignItems: 'center' },
    brandName: { fontSize: 46, fontWeight: '900', color: '#1F2937', letterSpacing: -1 },
    brandDot: { color: '#10B981' },
    subtitle: { fontSize: 16, color: '#6B7280', marginTop: 8, textAlign: 'center' },

    errorContainer: { backgroundColor: '#FEE2E2', padding: 12, borderRadius: 12, marginBottom: 16, borderWidth: 1, borderColor: '#FCA5A5' },
    errorText: { color: '#EF4444', fontSize: 14, textAlign: 'center', fontWeight: '500' },

    inputContainer: { gap: 16 },
    inputLabel: { fontSize: 14, fontWeight: '600', color: '#4B5563', marginBottom: 6, marginLeft: 4 },
    input: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, fontSize: 16, color: '#1F2937' },

    passwordContainer: { position: 'relative', justifyContent: 'center' },
    passwordInput: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, paddingRight: 50, fontSize: 16, color: '#1F2937' },
    eyeIcon: { position: 'absolute', right: 18, height: '100%', justifyContent: 'center', alignItems: 'center' },

    forgotPasswordButton: { alignSelf: 'flex-end', marginTop: 12, marginBottom: 24 },
    forgotPasswordText: { color: '#10B981', fontSize: 14, fontWeight: '600' },
    primaryButton: { backgroundColor: '#10B981', padding: 18, borderRadius: 16, alignItems: 'center' },
    primaryButtonText: { color: '#FFFFFF', fontSize: 18, fontWeight: '700' },
    footerContainer: { flexDirection: 'row', justifyContent: 'center', marginTop: 32 },
    footerText: { color: '#6B7280', fontSize: 15 },
    footerLink: { color: '#10B981', fontSize: 15, fontWeight: '700' },
});