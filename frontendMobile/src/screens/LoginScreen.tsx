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
import AsyncStorage from '@react-native-async-storage/async-storage'; // Dodany import
import { authService } from '../api/authClient.ts';
import { LoginRequest, Token } from '../types/auth.ts';
import axios from 'axios';
import { EyeIcon, EyeOffIcon } from '../components/icons/EyeIcons.tsx';
import { globalStyles } from '../styles/globalStyles.ts';

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
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <View style={styles.formContainer}>
          <View style={styles.headerContainer}>
            <Text style={styles.brandName}>
              Healthy<Text style={styles.brandDot}>.</Text>
            </Text>
            <Text style={styles.subtitle}>
              Witaj ponownie! Zaloguj się, aby kontynuować.
            </Text>
          </View>

          {errorMessage ? (
            <View style={globalStyles.errorContainer}>
              <Text style={globalStyles.errorText}>{errorMessage}</Text>
            </View>
          ) : null}

          <View style={globalStyles.inputContainer}>
            {/* Grupa Adres e-mail */}
            <View>
              <Text style={globalStyles.inputLabel}>Adres e-mail</Text>
              <TextInput
                style={globalStyles.input}
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
              <Text style={globalStyles.inputLabel}>Hasło</Text>
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

          <TouchableOpacity
            style={styles.primaryButton}
            onPress={handleLogin}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.primaryButtonText}>Zaloguj się</Text>
            )}
          </TouchableOpacity>

          <View style={styles.footerContainer}>
            <Text style={styles.footerText}>Nie masz jeszcze konta? </Text>
            <TouchableOpacity
              onPress={onNavigateToRegister}
              disabled={isLoading}
            >
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