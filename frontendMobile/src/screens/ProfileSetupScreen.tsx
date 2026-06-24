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
import DatePicker from 'react-native-date-picker';
import Svg, { Path, Polyline, Line } from 'react-native-svg';

// Importujemy naszego klienta API
import { patientClient } from '../api/patientClient';

// --- IKONA WYLOGOWANIA Z DASHBOARDU ---
const LogoutIcon = ({ color = "#EF4444", size = 24 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <Polyline points="16 17 21 12 16 7" />
        <Line x1="21" y1="12" x2="9" y2="12" />
    </Svg>
);

interface ProfileSetupScreenProps {
    patientData: any;
    onProfileUpdated: () => void;
    onLogout: () => void;
}

export const ProfileSetupScreen = ({ patientData, onProfileUpdated, onLogout }: ProfileSetupScreenProps) => {
    const [formData, setFormData] = useState({
        pesel: patientData?.pesel || '',
        phoneNumber: patientData?.phoneNumber || '',
        street: '',
        postalCode: '',
        city: '',
        dateOfBirth: patientData?.dateOfBirth || '',
    });

    const [isLoading, setIsLoading] = useState(false);
    const [openDateModal, setOpenDateModal] = useState(false);
    const [date, setDate] = useState(new Date(1990, 0, 1));

    // --- STANY DLA BANERÓW BŁĘDU/SUKCESU ---
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    // --- FUNKCJE WALIDACYJNE ---
    const validatePESEL = (pesel: string): boolean => {
        if (!/^\d{11}$/.test(pesel)) return false;

        const weights = [1, 3, 7, 9, 1, 3, 7, 9, 1, 3];
        let sum = 0;

        for (let i = 0; i < 10; i++) {
            sum += parseInt(pesel[i], 10) * weights[i];
        }

        const controlDigit = (10 - (sum % 10)) % 10;
        return controlDigit === parseInt(pesel[10], 10);
    };

    const validatePhoneNumber = (phone: string): boolean => {
        const cleaned = phone.replace(/[\s-]/g, '');
        return /^\d{9}$/.test(cleaned);
    };

    // --- OBSŁUGA FORMULARZA ---
    const handleSubmit = async () => {
        // Czyszczenie wiadomości przed każdą nową próbą
        setErrorMessage(null);
        setSuccessMessage(null);

        if (!formData.pesel || !formData.phoneNumber || !formData.street || !formData.postalCode || !formData.city || !formData.dateOfBirth) {
            setErrorMessage('Wszystkie pola są wymagane.');
            return;
        }

        if (!validatePESEL(formData.pesel)) {
            setErrorMessage('Podany numer PESEL jest niepoprawny.');
            return;
        }

        if (!validatePhoneNumber(formData.phoneNumber)) {
            setErrorMessage('Podany numer telefonu jest niepoprawny (wymagane 9 cyfr).');
            return;
        }

        setIsLoading(true);
        try {
            const fullAddress = `${formData.street.trim()}, ${formData.postalCode.trim()} ${formData.city.trim()}`;

            // Korzystamy z globalnego klienta z interceptorami
            await patientClient.updatePatient(patientData.id, {
                ...patientData,
                pesel: formData.pesel,
                phoneNumber: formData.phoneNumber.replace(/[\s-]/g, ''),
                dateOfBirth: formData.dateOfBirth,
                address: fullAddress
            });

            setSuccessMessage('Profil został pomyślnie zaktualizowany.');

            // Opóźnienie przed przekierowaniem
            setTimeout(() => {
                onProfileUpdated();
            }, 2000);

        } catch (error: any) {
            // Obsługa błędu formatu axiosa
            setErrorMessage(error.response?.data?.message || error.message || 'Nie udało się zapisać danych.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
            <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">

                {/* WYŚRODKOWANY NAGŁÓWEK Z IKONĄ WYLOGOWANIA PO PRAWEJ */}
                <View style={styles.headerRow} pointerEvents="box-none">
                    <Text style={styles.headerTitle}>Uzupełnij profil</Text>
                    <TouchableOpacity style={styles.headerLogoutButton} onPress={onLogout} activeOpacity={0.7} disabled={isLoading || !!successMessage}>
                        <LogoutIcon size={20} color="#EF4444" />
                    </TouchableOpacity>
                </View>

                <Text style={styles.subtitle}>Dzięki tym danym będziemy mogli lepiej dbać o Twoje zdrowie.</Text>

                {/* BANERY BŁĘDÓW I SUKCESÓW */}
                {errorMessage ? (
                    <View style={styles.errorContainer}>
                        <Text style={styles.errorText}>{errorMessage}</Text>
                    </View>
                ) : null}

                {successMessage ? (
                    <View style={styles.successContainer}>
                        <Text style={styles.successText}>{successMessage}</Text>
                    </View>
                ) : null}

                <View style={styles.inputContainer}>

                    {/* Data urodzenia */}
                    <View>
                        <Text style={styles.inputLabel}>Data urodzenia</Text>
                        <TouchableOpacity
                            style={styles.datePickerButton}
                            onPress={() => setOpenDateModal(true)}
                            disabled={isLoading || !!successMessage}
                        >
                            <Text style={[styles.datePickerText, !formData.dateOfBirth && styles.placeholderText]}>
                                {formData.dateOfBirth ? formData.dateOfBirth : "Wybierz datę"}
                            </Text>
                        </TouchableOpacity>
                    </View>

                    <DatePicker
                        modal
                        open={openDateModal}
                        date={date}
                        mode="date"
                        confirmText="Wybierz"
                        cancelText="Anuluj"
                        title="Wybierz datę urodzenia"
                        maximumDate={new Date()}
                        onConfirm={(selectedDate) => {
                            setOpenDateModal(false);
                            setDate(selectedDate);
                            // Pobieramy YYYY-MM-DD unikając stref czasowych, bazując na lokalnym wyborze
                            const offset = selectedDate.getTimezoneOffset();
                            const adjustedDate = new Date(selectedDate.getTime() - (offset * 60 * 1000));
                            const formattedDate = adjustedDate.toISOString().split('T')[0];
                            setFormData({...formData, dateOfBirth: formattedDate});
                        }}
                        onCancel={() => {
                            setOpenDateModal(false);
                        }}
                    />

                    {/* PESEL */}
                    <View>
                        <Text style={styles.inputLabel}>Numer PESEL</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="11 cyfr"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="numeric"
                            maxLength={11}
                            value={formData.pesel}
                            onChangeText={(v) => setFormData({...formData, pesel: v.replace(/\D/g, '')})}
                            editable={!isLoading && !successMessage}
                        />
                    </View>

                    {/* Numer telefonu */}
                    <View>
                        <Text style={styles.inputLabel}>Numer telefonu</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="9 cyfr, np. 123456789"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="phone-pad"
                            maxLength={12}
                            value={formData.phoneNumber}
                            onChangeText={(v) => setFormData({...formData, phoneNumber: v})}
                            editable={!isLoading && !successMessage}
                        />
                    </View>

                    {/* Sekcja adresu */}
                    <Text style={styles.sectionLabel}>Adres zamieszkania</Text>

                    {/* Ulica */}
                    <View>
                        <Text style={styles.inputLabel}>Ulica i numer domu/mieszkania</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="np. Kwiatowa 12/4"
                            placeholderTextColor="#9CA3AF"
                            value={formData.street}
                            onChangeText={(v) => setFormData({...formData, street: v})}
                            editable={!isLoading && !successMessage}
                        />
                    </View>

                    {/* Kod pocztowy i Miasto */}
                    <View style={styles.row}>
                        <View style={{ flex: 1 }}>
                            <Text style={styles.inputLabel}>Kod pocztowy</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="00-000"
                                placeholderTextColor="#9CA3AF"
                                maxLength={6}
                                value={formData.postalCode}
                                onChangeText={(v) => setFormData({...formData, postalCode: v})}
                                editable={!isLoading && !successMessage}
                            />
                        </View>
                        <View style={{ flex: 2 }}>
                            <Text style={styles.inputLabel}>Miejscowość</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="Nazwa miejscowości"
                                placeholderTextColor="#9CA3AF"
                                value={formData.city}
                                onChangeText={(v) => setFormData({...formData, city: v})}
                                editable={!isLoading && !successMessage}
                            />
                        </View>
                    </View>
                </View>

                {/* Główny przycisk */}
                <TouchableOpacity
                    style={[styles.primaryButton, (isLoading || !!successMessage) && styles.primaryButtonDisabled]}
                    onPress={handleSubmit}
                    disabled={isLoading || !!successMessage}
                >
                    {isLoading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryButtonText}>Zapisz dane</Text>}
                </TouchableOpacity>

            </ScrollView>
        </KeyboardAvoidingView>
    );
};

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FFFFFF' },
    scrollContainer: { flexGrow: 1, justifyContent: 'center', paddingHorizontal: 32, paddingVertical: 40 },

    headerRow: {
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 12,
        position: 'relative',
        minHeight: 44,
    },
    headerTitle: {
        fontSize: 30,
        fontWeight: '900',
        color: '#1F2937',
        textAlign: 'center',
    },
    headerLogoutButton: {
        position: 'absolute',
        right: 0,
        padding: 10,
        backgroundColor: '#FEE2E2',
        borderRadius: 12,
        zIndex: 10,
        elevation: 5,
    },

    subtitle: { fontSize: 16, color: '#6B7280', marginBottom: 32, textAlign: 'center' },

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
    successContainer: {
        backgroundColor: '#D1FAE5',
        padding: 12,
        borderRadius: 12,
        marginBottom: 16,
        borderWidth: 1,
        borderColor: '#6EE7B7',
    },
    successText: {
        color: '#065F46',
        fontSize: 14,
        textAlign: 'center',
        fontWeight: '600',
    },

    inputContainer: { gap: 16, marginBottom: 32 },
    inputLabel: { fontSize: 14, fontWeight: '600', color: '#4B5563', marginBottom: 6, marginLeft: 4 },
    input: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, fontSize: 16, color: '#1F2937' },

    datePickerButton: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, justifyContent: 'center' },
    datePickerText: { fontSize: 16, color: '#1F2937' },
    placeholderText: { color: '#9CA3AF' },

    sectionLabel: { fontSize: 18, fontWeight: '800', color: '#1F2937', marginTop: 16, marginBottom: 8, marginLeft: 4 },
    row: { flexDirection: 'row', gap: 16 },

    primaryButton: {
        backgroundColor: '#10B981',
        padding: 18,
        borderRadius: 16,
        alignItems: 'center',
        elevation: 4,
        marginBottom: 16,
    },
    primaryButtonDisabled: { backgroundColor: '#9CA3AF', shadowOpacity: 0, elevation: 0 },
    primaryButtonText: { color: '#FFFFFF', fontSize: 18, fontWeight: '700' },
});