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
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import DatePicker from 'react-native-date-picker';
import Svg, { Path } from 'react-native-svg';

// Importujemy naszego klienta API
import { patientClient } from '../api/patientClient';

// --- IKONA POWROTU ---
const BackIcon = ({ color = "#1F2937", size = 26 }) => (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M15 18l-6-6 6-6" />
    </Svg>
);

export const ProfileEditScreen = ({ route, navigation }: any) => {
    const insets = useSafeAreaInsets();

    // Usunięto token - apiClient sam zarządza nagłówkami
    const { patientData } = route.params || {};

    // Rozbicie adresu na ulicę, kod i miasto dla formularza
    const addressParts = patientData?.address ? patientData.address.split(',') : ['', ''];
    const streetPart = addressParts[0] ? addressParts[0].trim() : '';
    const codeAndCity = addressParts[1] ? addressParts[1].trim().split(' ') : ['', ''];
    const codePart = codeAndCity[0] ? codeAndCity[0].trim() : '';
    const cityPart = codeAndCity.slice(1).join(' ').trim();

    const [formData, setFormData] = useState({
        firstName: patientData?.firstName || '',
        lastName: patientData?.lastName || '',
        email: patientData?.email || '',
        pesel: patientData?.pesel || '',
        phoneNumber: patientData?.phoneNumber || '',
        street: streetPart,
        postalCode: codePart,
        city: cityPart,
        dateOfBirth: patientData?.dateOfBirth || '',
    });

    const [isLoading, setIsLoading] = useState(false);
    const [openDateModal, setOpenDateModal] = useState(false);
    const [date, setDate] = useState(patientData?.dateOfBirth ? new Date(patientData.dateOfBirth) : new Date(1990, 0, 1));

    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

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
        // Backend DTO allows ^\+?[0-9]{9,15}$
        return /^\+?\d{9,15}$/.test(cleaned);
    };

    const handleSubmit = async () => {
        setErrorMessage(null);
        setSuccessMessage(null);

        // Walidacja obecności
        if (!formData.firstName || !formData.lastName || !formData.email || !formData.pesel || !formData.phoneNumber || !formData.street || !formData.postalCode || !formData.city || !formData.dateOfBirth) {
            setErrorMessage('Wszystkie pola są wymagane.');
            return;
        }

        if (!validatePESEL(formData.pesel)) {
            setErrorMessage('Podany numer PESEL jest niepoprawny.');
            return;
        }

        if (!validatePhoneNumber(formData.phoneNumber)) {
            setErrorMessage('Podany numer telefonu jest niepoprawny.');
            return;
        }

        setIsLoading(true);
        try {
            // Łączenie 3 pól w jeden pełny adres przed wysłaniem
            const fullAddress = `${formData.street.trim()}, ${formData.postalCode.trim()} ${formData.city.trim()}`;

            // Korzystamy z nowo utworzonego klienta omijając surowy 'fetch'
            await patientClient.updatePatient(patientData.id, {
                firstName: formData.firstName,
                lastName: formData.lastName,
                email: formData.email,
                pesel: formData.pesel,
                phoneNumber: formData.phoneNumber.replace(/[\s-]/g, ''),
                dateOfBirth: formData.dateOfBirth,
                address: fullAddress
            });

            setSuccessMessage('Profil został pomyślnie zaktualizowany.');

            // Opóźnienie przed wróceniem do Dashboardu
            setTimeout(() => {
                navigation.goBack();
            }, 1500);

        } catch (error: any) {
            // Obsługa błędu formatu axiosa
            setErrorMessage(error.response?.data?.message || error.message || 'Nie udało się zaktualizować danych.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
            <ScrollView
                contentContainerStyle={[styles.scrollContainer, { paddingTop: insets.top + 20 }]}
                keyboardShouldPersistTaps="handled"
                showsVerticalScrollIndicator={false}
            >

                {/* NAGŁÓWEK ZE STRZAŁKĄ POWROTU */}
                <View style={styles.headerRow}>
                    <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()} disabled={isLoading || !!successMessage}>
                        <BackIcon />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>Edycja profilu</Text>
                </View>

                <Text style={styles.subtitle}>Zaktualizuj swoje dane podstawowe, medyczne i adresowe.</Text>

                {errorMessage && (
                    <View style={styles.errorContainer}>
                        <Text style={styles.errorText}>{errorMessage}</Text>
                    </View>
                )}

                {successMessage && (
                    <View style={styles.successContainer}>
                        <Text style={styles.successText}>{successMessage}</Text>
                    </View>
                )}

                <View style={styles.inputContainer}>

                    <Text style={styles.sectionLabel}>Dane podstawowe</Text>

                    {/* Imię */}
                    <View>
                        <Text style={styles.inputLabel}>Imię</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="Wpisz imię"
                            placeholderTextColor="#9CA3AF"
                            value={formData.firstName}
                            onChangeText={(v) => setFormData({...formData, firstName: v})}
                            editable={!isLoading && !successMessage}
                        />
                    </View>

                    {/* Nazwisko */}
                    <View>
                        <Text style={styles.inputLabel}>Nazwisko</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="Wpisz nazwisko"
                            placeholderTextColor="#9CA3AF"
                            value={formData.lastName}
                            onChangeText={(v) => setFormData({...formData, lastName: v})}
                            editable={!isLoading && !successMessage}
                        />
                    </View>

                    {/* E-mail */}
                    <View>
                        <Text style={styles.inputLabel}>Adres e-mail</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="np. jan@kowalski.pl"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="email-address"
                            autoCapitalize="none"
                            value={formData.email}
                            onChangeText={(v) => setFormData({...formData, email: v})}
                            editable={!isLoading && !successMessage}
                        />
                    </View>

                    <Text style={styles.sectionLabel}>Szczegóły medyczne</Text>

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
                            const adjustedDate = new Date(selectedDate.getTime() - (offset*60*1000));
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

                    {/* Adres */}
                    <Text style={styles.sectionLabel}>Adres zamieszkania</Text>

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
                    {isLoading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryButtonText}>Zapisz zmiany</Text>}
                </TouchableOpacity>

            </ScrollView>
        </KeyboardAvoidingView>
    );
};

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FFFFFF' },
    scrollContainer: { flexGrow: 1, paddingHorizontal: 24, paddingBottom: 40 },

    headerRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 12,
    },
    backButton: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: '#F3F4F6',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 16
    },
    headerTitle: {
        fontSize: 28,
        fontWeight: '900',
        color: '#1F2937',
    },

    subtitle: { fontSize: 16, color: '#6B7280', marginBottom: 24 },

    errorContainer: { backgroundColor: '#FEE2E2', padding: 12, borderRadius: 12, marginBottom: 16, borderWidth: 1, borderColor: '#FCA5A5' },
    errorText: { color: '#EF4444', fontSize: 14, textAlign: 'center', fontWeight: '500' },

    successContainer: { backgroundColor: '#D1FAE5', padding: 12, borderRadius: 12, marginBottom: 16, borderWidth: 1, borderColor: '#6EE7B7' },
    successText: { color: '#065F46', fontSize: 14, textAlign: 'center', fontWeight: '600' },

    inputContainer: { gap: 16, marginBottom: 32 },
    sectionLabel: { fontSize: 18, fontWeight: '800', color: '#1F2937', marginTop: 16, marginBottom: 4, marginLeft: 4 },

    inputLabel: { fontSize: 14, fontWeight: '600', color: '#4B5563', marginBottom: 6, marginLeft: 4 },
    input: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, fontSize: 16, color: '#1F2937' },

    datePickerButton: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, justifyContent: 'center' },
    datePickerText: { fontSize: 16, color: '#1F2937' },
    placeholderText: { color: '#9CA3AF' },

    row: { flexDirection: 'row', gap: 16 },

    primaryButton: { backgroundColor: '#10B981', padding: 18, borderRadius: 16, alignItems: 'center', elevation: 4, marginBottom: 16 },
    primaryButtonDisabled: { backgroundColor: '#9CA3AF', shadowOpacity: 0, elevation: 0 },
    primaryButtonText: { color: '#FFFFFF', fontSize: 18, fontWeight: '700' },
});