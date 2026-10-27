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
    ScrollView,
} from 'react-native';
import {PatientRegistrationRequest} from "../types/auth.ts";
import {authService} from "../api/authClient.ts";
import axios from 'axios';
import { globalStyles } from '../styles/globalStyles';
import { EyeIcon, EyeOffIcon } from '../components/icons/EyeIcons.tsx';


interface RegisterScreenProps {
    onNavigateToLogin: () => void;
}

export const RegisterScreen = ({ onNavigateToLogin }: RegisterScreenProps) => {
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');

    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const handleRegister = async () => {
        setErrorMessage(null);
        setSuccessMessage(null);

        const trimmedFirstName = firstName.trim();
        const trimmedLastName = lastName.trim();
        const trimmedEmail = email.trim();

        // 1. Sprawdzenie, czy pola nie są puste
        if (!trimmedFirstName || !trimmedLastName || !trimmedEmail || !password || !confirmPassword) {
            setErrorMessage('Wypełnij wszystkie pola formularza.');
            return;
        }

        // Wyrażenia regularne zgodne z DTO
        const nameRegex = /^[A-Za-zÀ-ÿĄąĆćĘęŁłŃńÓóŚśŹźŻż\- ]+$/;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&._#-])[A-Za-z\d@$!%*?&._#-]{8,64}$/;

        // 2. Walidacja imienia
        if (trimmedFirstName.length < 2 || trimmedFirstName.length > 50) {
            setErrorMessage('Imię musi mieć od 2 do 50 znaków.');
            return;
        }
        if (!nameRegex.test(trimmedFirstName)) {
            setErrorMessage('Imię może zawierać tylko litery, spacje i myślniki.');
            return;
        }

        // 3. Walidacja nazwiska
        if (trimmedLastName.length < 2 || trimmedLastName.length > 50) {
            setErrorMessage('Nazwisko musi mieć od 2 do 50 znaków.');
            return;
        }
        if (!nameRegex.test(trimmedLastName)) {
            setErrorMessage('Nazwisko może zawierać tylko litery, spacje i myślniki.');
            return;
        }

        // 4. Walidacja e-maila
        if (!emailRegex.test(trimmedEmail)) {
            setErrorMessage('Nieprawidłowy format adresu e-mail.');
            return;
        }

        // 5. Walidacja hasła
        if (password.length < 8 || password.length > 64) {
            setErrorMessage('Hasło musi mieć od 8 do 64 znaków.');
            return;
        }
        if (!passwordRegex.test(password)) {
            setErrorMessage('Hasło musi zawierać co najmniej jedną wielką i małą literę, cyfrę oraz znak specjalny.');
            return;
        }

        // 6. Potwierdzenie hasła
        if (password !== confirmPassword) {
            setErrorMessage('Podane hasła nie są identyczne.');
            return;
        }

        setIsLoading(true);

        try {
            const requestData: PatientRegistrationRequest = {
              firstName: trimmedFirstName,
              lastName: trimmedLastName,
              email: trimmedEmail,
              password: password,
            };

            await authService.registerPatient(requestData);

            // Pokazujemy komunikat sukcesu i czyścimy formularz
            setSuccessMessage('Konto utworzone pomyślnie!');
            setFirstName('');
            setLastName('');
            setEmail('');
            setPassword('');
            setConfirmPassword('');

            // Automatyczne przejście do logowania po 2 sekundach
            setTimeout(() => {
                onNavigateToLogin();
            }, 2000);

        } catch (error: any) {
            if (axios.isAxiosError(error)) {

              if (error.response?.status === 409) {
                setErrorMessage('Użytkownik z tym adresem e-mail już istnieje.');
              } else {
                  setErrorMessage('Wystąpił błąd podczas rejestracji. Spróbuj ponownie.');
              }
            } else {
              setErrorMessage(
                'Brak połączenia z serwerem lub nieoczekiwany błąd.',
              );
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
        <ScrollView
          contentContainerStyle={styles.scrollContainer}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.headerContainer}>
            <Text style={styles.brandName}>
              Healthy<Text style={styles.brandDot}>.</Text>
            </Text>
            <Text style={styles.subtitle}>
              Dołącz do nas i zacznij dbać o swoje zdrowie już dziś.
            </Text>
          </View>

          {/* BANER BŁĘDU */}
          {errorMessage ? (
            <View style={globalStyles.errorContainer}>
              <Text style={globalStyles.errorText}>{errorMessage}</Text>
            </View>
          ) : null}

          {/* BANER SUKCESU */}
          {successMessage ? (
            <View style={globalStyles.successContainer}>
              <Text style={globalStyles.successText}>{successMessage}</Text>
            </View>
          ) : null}

          <View style={globalStyles.inputContainer}>
            {/* Imię */}
            <View>
              <Text style={globalStyles.inputLabel}>Imię</Text>
              <TextInput
                style={globalStyles.input}
                placeholder="Wpisz swoje imię"
                placeholderTextColor="#9CA3AF"
                value={firstName}
                onChangeText={setFirstName}
                editable={!isLoading && !successMessage}
              />
            </View>

            {/* Nazwisko */}
            <View>
              <Text style={globalStyles.inputLabel}>Nazwisko</Text>
              <TextInput
                style={globalStyles.input}
                placeholder="Wpisz swoje nazwisko"
                placeholderTextColor="#9CA3AF"
                value={lastName}
                onChangeText={setLastName}
                editable={!isLoading && !successMessage}
              />
            </View>

            {/* Adres e-mail */}
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
                editable={!isLoading && !successMessage}
              />
            </View>

            {/* Hasło */}
            <View>
              <Text style={globalStyles.inputLabel}>Hasło</Text>
              <View style={styles.passwordContainer}>
                <TextInput
                  style={styles.passwordInput}
                  placeholder="Utwórz hasło"
                  placeholderTextColor="#9CA3AF"
                  secureTextEntry={!showPassword}
                  value={password}
                  onChangeText={setPassword}
                  editable={!isLoading && !successMessage}
                />
                <TouchableOpacity
                  style={styles.eyeIcon}
                  onPress={() => setShowPassword(!showPassword)}
                  disabled={isLoading || !!successMessage}
                  hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </TouchableOpacity>
              </View>
            </View>

            {/* Powtórz Hasło */}
            <View>
              <Text style={globalStyles.inputLabel}>Powtórz hasło</Text>
              <View style={styles.passwordContainer}>
                <TextInput
                  style={styles.passwordInput}
                  placeholder="Wpisz hasło ponownie"
                  placeholderTextColor="#9CA3AF"
                  secureTextEntry={!showConfirmPassword}
                  value={confirmPassword}
                  onChangeText={setConfirmPassword}
                  editable={!isLoading && !successMessage}
                />
                <TouchableOpacity
                  style={styles.eyeIcon}
                  onPress={() => setShowConfirmPassword(!showConfirmPassword)}
                  disabled={isLoading || !!successMessage}
                  hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                >
                  {showConfirmPassword ? <EyeOffIcon /> : <EyeIcon />}
                </TouchableOpacity>
              </View>
            </View>
          </View>

          <TouchableOpacity
            style={[
              styles.primaryButton,
              (isLoading || !!successMessage) && styles.primaryButtonDisabled,
            ]}
            onPress={handleRegister}
            disabled={isLoading || !!successMessage}
          >
            {isLoading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.primaryButtonText}>Utwórz konto</Text>
            )}
          </TouchableOpacity>

          <View style={styles.footerContainer}>
            <Text style={styles.footerText}>Masz już konto? </Text>
            <TouchableOpacity
              onPress={() => {
                setErrorMessage(null);
                setSuccessMessage(null);
                onNavigateToLogin();
              }}
              disabled={isLoading || !!successMessage}
            >
              <Text style={styles.footerLink}>Zaloguj się</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    );
};

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FFFFFF' },
    scrollContainer: { flexGrow: 1, justifyContent: 'center', paddingHorizontal: 32, paddingVertical: 40 },
    headerContainer: { marginBottom: 32, alignItems: 'center' },
    brandName: { fontSize: 46, fontWeight: '900', color: '#1F2937', letterSpacing: -1 },
    brandDot: { color: '#10B981' },
    subtitle: { fontSize: 16, color: '#6B7280', marginTop: 8, textAlign: 'center', lineHeight: 22 },

    passwordContainer: {
        position: 'relative',
        justifyContent: 'center',
    },
    passwordInput: {
        backgroundColor: '#F3F4F6',
        borderRadius: 16,
        padding: 18,
        paddingRight: 50,
        fontSize: 16,
        color: '#1F2937',
    },
    eyeIcon: {
        position: 'absolute',
        right: 18,
        height: '100%',
        justifyContent: 'center',
        alignItems: 'center',
    },

    primaryButton: {
        backgroundColor: '#1F2937',
        padding: 18,
        borderRadius: 16,
        alignItems: 'center',
        shadowColor: '#1F2937',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.2,
        shadowRadius: 6,
        elevation: 4,
    },
    primaryButtonDisabled: { backgroundColor: '#9CA3AF', shadowOpacity: 0 },
    primaryButtonText: { color: '#FFFFFF', fontSize: 18, fontWeight: '700' },
    footerContainer: { flexDirection: 'row', justifyContent: 'center', marginTop: 32 },
    footerText: { color: '#6B7280', fontSize: 15 },
    footerLink: { color: '#10B981', fontSize: 15, fontWeight: '700' },
});