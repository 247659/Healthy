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
    Alert,
} from 'react-native';
import Svg, { Path, Circle, Line } from 'react-native-svg';

const AUTH_SERVICE_URL = 'http://10.0.2.2:8087/api/v1/auth';

// Dodaj ten interfejs:
interface IconProps {
    color?: string;
    size?: number;
}

// Zaktualizuj definicje ikon dodając ": IconProps":
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
// ----------------------------

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

    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const handleRegister = async () => {
        setErrorMessage(null);

        if (!firstName || !lastName || !email || !password || !confirmPassword) {
            setErrorMessage('Wypełnij wszystkie pola formularza.');
            return;
        }
        if (password !== confirmPassword) {
            setErrorMessage('Podane hasła nie są identyczne.');
            return;
        }

        setIsLoading(true);

        try {
            const response = await fetch(`${AUTH_SERVICE_URL}/register/patient`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    firstName: firstName.trim(),
                    lastName: lastName.trim(),
                    email: email.trim(),
                    password
                }),
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Wystąpił błąd podczas rejestracji.');
            }

            Alert.alert('Sukces', 'Konto utworzone! Możesz się teraz zalogować.');
            onNavigateToLogin();

        } catch (error: any) {
            setErrorMessage(error.message || 'Brak połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
            <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
                <View style={styles.headerContainer}>
                    <Text style={styles.brandName}>Healthy<Text style={styles.brandDot}>.</Text></Text>
                    <Text style={styles.subtitle}>Dołącz do nas i zacznij dbać o swoje zdrowie już dziś.</Text>
                </View>

                {errorMessage ? (
                    <View style={styles.errorContainer}>
                        <Text style={styles.errorText}>{errorMessage}</Text>
                    </View>
                ) : null}

                <View style={styles.inputContainer}>
                    <TextInput
                        style={styles.input}
                        placeholder="Imię"
                        placeholderTextColor="#9CA3AF"
                        value={firstName}
                        onChangeText={setFirstName}
                        editable={!isLoading}
                    />

                    <TextInput
                        style={styles.input}
                        placeholder="Nazwisko"
                        placeholderTextColor="#9CA3AF"
                        value={lastName}
                        onChangeText={setLastName}
                        editable={!isLoading}
                    />

                    <TextInput
                        style={styles.input}
                        placeholder="Adres e-mail"
                        placeholderTextColor="#9CA3AF"
                        keyboardType="email-address"
                        autoCapitalize="none"
                        value={email}
                        onChangeText={setEmail}
                        editable={!isLoading}
                    />

                    {/* Pole hasła z ikoną SVG */}
                    <View style={styles.passwordContainer}>
                        <TextInput
                            style={styles.passwordInput}
                            placeholder="Utwórz hasło"
                            placeholderTextColor="#9CA3AF"
                            secureTextEntry={!showPassword}
                            value={password}
                            onChangeText={setPassword}
                            editable={!isLoading}
                        />
                        <TouchableOpacity
                            style={styles.eyeIcon}
                            onPress={() => setShowPassword(!showPassword)}
                            disabled={isLoading}
                            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }} // Powiększa obszar kliknięcia ikony
                        >
                            {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                        </TouchableOpacity>
                    </View>

                    {/* Pole powtórzenia hasła z ikoną SVG */}
                    <View style={styles.passwordContainer}>
                        <TextInput
                            style={styles.passwordInput}
                            placeholder="Powtórz hasło"
                            placeholderTextColor="#9CA3AF"
                            secureTextEntry={!showConfirmPassword}
                            value={confirmPassword}
                            onChangeText={setConfirmPassword}
                            editable={!isLoading}
                        />
                        <TouchableOpacity
                            style={styles.eyeIcon}
                            onPress={() => setShowConfirmPassword(!showConfirmPassword)}
                            disabled={isLoading}
                            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                        >
                            {showConfirmPassword ? <EyeOffIcon /> : <EyeIcon />}
                        </TouchableOpacity>
                    </View>
                </View>

                <TouchableOpacity
                    style={[styles.primaryButton, isLoading && styles.primaryButtonDisabled]}
                    onPress={handleRegister}
                    disabled={isLoading}
                >
                    {isLoading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryButtonText}>Utwórz konto</Text>}
                </TouchableOpacity>

                <View style={styles.footerContainer}>
                    <Text style={styles.footerText}>Masz już konto? </Text>
                    <TouchableOpacity onPress={() => {
                        setErrorMessage(null);
                        onNavigateToLogin();
                    }} disabled={isLoading}>
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

    errorContainer: {
        backgroundColor: '#FEE2E2',
        padding: 12,
        borderRadius: 12,
        marginBottom: 16,
        borderWidth: 1,
        borderColor: '#FCA5A5',
    },
    errorText: {
        color: '#EF4444',
        fontSize: 14,
        textAlign: 'center',
        fontWeight: '500',
    },

    inputContainer: { gap: 16, marginBottom: 32 },
    input: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, fontSize: 16, color: '#1F2937' },

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