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
import DatePicker from 'react-native-date-picker'; // NOWY IMPORT

const API_URL = 'http://10.0.2.2:8088'; // Zmień na 8088 jeśli tego używasz

interface ProfileSetupScreenProps {
    patientData: any;
    onProfileUpdated: () => void;
}

export const ProfileSetupScreen = ({ patientData, onProfileUpdated }: ProfileSetupScreenProps) => {
    // Rozbicie adresu na osobne pola i dodanie stanu dla daty
    const [formData, setFormData] = useState({
        pesel: patientData?.pesel || '',
        phoneNumber: patientData?.phoneNumber || '',
        street: '',
        postalCode: '',
        city: '',
        dateOfBirth: patientData?.dateOfBirth || '',
    });

    const [isLoading, setIsLoading] = useState(false);

    // Stany do obsługi modala z datą
    const [openDateModal, setOpenDateModal] = useState(false);
    const [date, setDate] = useState(new Date(1990, 0, 1)); // Domyślna data startowa

    const handleSubmit = async () => {
        // 1. Sprawdzamy czy wszystkie pola są wypełnione
        if (!formData.pesel || !formData.phoneNumber || !formData.street || !formData.postalCode || !formData.city || !formData.dateOfBirth) {
            Alert.alert('Błąd', 'Wszystkie pola są wymagane.');
            return;
        }

        setIsLoading(true);
        try {
            // Łączenie adresu w jeden string dla backendu: "Ulica 12, 00-000 Miasto"
            const fullAddress = `${formData.street.trim()}, ${formData.postalCode.trim()} ${formData.city.trim()}`;

            const response = await fetch(`${API_URL}/api/v1/patients/update/${patientData.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    // 'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    ...patientData,
                    pesel: formData.pesel,
                    phoneNumber: formData.phoneNumber,
                    dateOfBirth: formData.dateOfBirth,
                    address: fullAddress // Wysyłamy złączony adres
                }),
            });

            if (!response.ok) throw new Error('Nie udało się zapisać danych. Sprawdź poprawność wprowadzonych wartości.');

            Alert.alert('Sukces', 'Profil został pomyślnie zaktualizowany.');
            onProfileUpdated();
        } catch (error: any) {
            Alert.alert('Błąd', error.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
            <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
                <Text style={styles.header}>Uzupełnij profil</Text>
                <Text style={styles.subtitle}>Dzięki tym danym będziemy mogli lepiej dbać o Twoje zdrowie.</Text>

                <View style={styles.inputContainer}>

                    {/* MODAL I POLE: Data urodzenia */}
                    <TouchableOpacity
                        style={styles.datePickerButton}
                        onPress={() => setOpenDateModal(true)}
                    >
                        <Text style={[styles.datePickerText, !formData.dateOfBirth && styles.placeholderText]}>
                            {formData.dateOfBirth ? formData.dateOfBirth : "Data urodzenia (Wybierz)"}
                        </Text>
                    </TouchableOpacity>

                    <DatePicker
                        modal
                        open={openDateModal}
                        date={date}
                        mode="date"
                        confirmText="Wybierz"
                        cancelText="Anuluj"
                        title="Wybierz datę urodzenia"
                        maximumDate={new Date()} // Nie można wybrać daty z przyszłości
                        onConfirm={(selectedDate) => {
                            setOpenDateModal(false);
                            setDate(selectedDate);
                            // Formatowanie daty do YYYY-MM-DD bezpieczne dla backendu
                            const formattedDate = selectedDate.toISOString().split('T')[0];
                            setFormData({...formData, dateOfBirth: formattedDate});
                        }}
                        onCancel={() => {
                            setOpenDateModal(false);
                        }}
                    />

                    <TextInput
                        style={styles.input}
                        placeholder="PESEL (11 cyfr)"
                        placeholderTextColor="#9CA3AF"
                        keyboardType="numeric"
                        maxLength={11}
                        value={formData.pesel}
                        onChangeText={(v) => setFormData({...formData, pesel: v})}
                    />
                    <TextInput
                        style={styles.input}
                        placeholder="Numer telefonu"
                        placeholderTextColor="#9CA3AF"
                        keyboardType="phone-pad"
                        value={formData.phoneNumber}
                        onChangeText={(v) => setFormData({...formData, phoneNumber: v})}
                    />

                    {/* ROZDZIELONY ADRES */}
                    <Text style={styles.sectionLabel}>Adres zamieszkania</Text>

                    <TextInput
                        style={styles.input}
                        placeholder="Ulica i numer (np. Kwiatowa 12/4)"
                        placeholderTextColor="#9CA3AF"
                        value={formData.street}
                        onChangeText={(v) => setFormData({...formData, street: v})}
                    />

                    <View style={styles.row}>
                        <TextInput
                            style={[styles.input, { flex: 1 }]} // Kod pocztowy zajmuje mniej miejsca
                            placeholder="Kod (00-000)"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="numeric"
                            maxLength={6}
                            value={formData.postalCode}
                            onChangeText={(v) => setFormData({...formData, postalCode: v})}
                        />
                        <TextInput
                            style={[styles.input, { flex: 2 }]} // Miasto zajmuje więcej miejsca
                            placeholder="Miejscowość"
                            placeholderTextColor="#9CA3AF"
                            value={formData.city}
                            onChangeText={(v) => setFormData({...formData, city: v})}
                        />
                    </View>
                </View>

                <TouchableOpacity style={styles.primaryButton} onPress={handleSubmit} disabled={isLoading}>
                    {isLoading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryButtonText}>Zapisz dane</Text>}
                </TouchableOpacity>
            </ScrollView>
        </KeyboardAvoidingView>
    );
};

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FFFFFF' },
    scrollContainer: { flexGrow: 1, justifyContent: 'center', paddingHorizontal: 32, paddingVertical: 40 },
    header: { fontSize: 32, fontWeight: '900', color: '#1F2937', marginBottom: 12, textAlign: 'center' },
    subtitle: { fontSize: 16, color: '#6B7280', marginBottom: 32, textAlign: 'center' },

    inputContainer: { gap: 16, marginBottom: 32 },
    input: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, fontSize: 16, color: '#1F2937' },

    // Style dla wyboru daty
    datePickerButton: { backgroundColor: '#F3F4F6', borderRadius: 16, padding: 18, justifyContent: 'center' },
    datePickerText: { fontSize: 16, color: '#1F2937' },
    placeholderText: { color: '#9CA3AF' },

    // Style dla sekcji adresu
    sectionLabel: { fontSize: 14, fontWeight: '700', color: '#6B7280', marginTop: 8, marginLeft: 4 },
    row: { flexDirection: 'row', gap: 16 },

    primaryButton: {
        backgroundColor: '#10B981',
        padding: 18,
        borderRadius: 16,
        alignItems: 'center',
        shadowColor: '#10B981',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 6,
        elevation: 4,
    },
    primaryButtonText: { color: '#FFFFFF', fontSize: 18, fontWeight: '700' },
});