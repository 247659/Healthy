import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { medicalStaffService } from "../api/staffClient.ts";
import type { MedicalStaff } from "../types/medicalStaff.ts";

// Pomocniczy interfejs dla stanu specjalizacji
interface SpecState {
    name: string;
    obtainedDate: string;
    certificateNumber: string;
}

const StaffProfile = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    // Dane podstawowe
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [licenseNumber, setLicenseNumber] = useState('');

    // Stan dla wielu specjalizacji
    const [specializations, setSpecializations] = useState<SpecState[]>([
        { name: '', obtainedDate: '', certificateNumber: '' }
    ]);

    // Stan dla zmiany hasła
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');

    const [isEditing, setIsEditing] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const token = localStorage.getItem('access_token');

    const fetchProfile = useCallback(async () => {
        try {
            const response = await medicalStaffService.getMedicalStaffInformationById(id as string);

            setFirstName(response.firstName);
            setLastName(response.lastName);
            setPhoneNumber(response.phoneNumber);
            setLicenseNumber(response.licenseNumber);

            // Wczytanie specjalizacji (lub ustawienie jednej pustej jako domyślnej)
            if (response.specializations && response.specializations.length > 0) {
                setSpecializations(response.specializations);
            } else {
                setSpecializations([{ name: '', obtainedDate: '', certificateNumber: '' }]);
            }
            setIsLoading(false);
        } catch (err) {
            console.error("Błąd pobierania profilu:", err);
            setError("Nie udało się załadować danych profilu.");
            setIsLoading(false);
        }
    }, [id]);

    useEffect(() => {
        if (id) {
            fetchProfile();
        }
    }, [fetchProfile, token]);

    // --- Funkcje obsługi specjalizacji ---
    const handleSpecChange = (index: number, field: keyof SpecState, value: string) => {
        const updatedSpecs = [...specializations];
        updatedSpecs[index] = { ...updatedSpecs[index], [field]: value };
        setSpecializations(updatedSpecs);
    };

    const addSpecialization = () => {
        setSpecializations([...specializations, { name: '', obtainedDate: '', certificateNumber: '' }]);
    };

    const removeSpecialization = (index: number) => {
        const updatedSpecs = specializations.filter((_, i) => i !== index);
        setSpecializations(updatedSpecs);
    };

    // --- Funkcje obsługi formularza ---
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');

        // Weryfikacja haseł TYLKO jeśli użytkownik chce je zmienić
        if (password || confirmPassword) {
            if (password !== confirmPassword) {
                setError('Podane hasła nie są identyczne. Spróbuj ponownie.');
                return;
            }
        }

        setIsLoading(true);

        try {
            if (id) {
                const data: MedicalStaff = {
                    id: id,
                    firstName: firstName,
                    password: password, // Wyślemy nowe hasło, albo pusty string
                    lastName: lastName,
                    phoneNumber: phoneNumber,
                    licenseNumber: licenseNumber,
                    specializations: specializations
                }

                await medicalStaffService.editProfile(id, data);

                setSuccessMessage('Profil został pomyślnie zaktualizowany!');
                setIsEditing(false);
                setPassword('');
                setConfirmPassword('');
            }
        } catch (err: any) {
            console.error('Błąd aktualizacji:', err);
            const backendMsg = err.response?.data?.message || 'Sprawdź poprawność danych (np. 7 cyfr dla numerów).';
            setError(`Nie udało się zaktualizować profilu. ${backendMsg}`);
        } finally {
            setIsLoading(false);
        }
    };

    const handleCancel = () => {
        setIsEditing(false);
        setPassword('');
        setConfirmPassword('');
        setError('');
        setIsLoading(true);
        fetchProfile(); // Reset danych do stanu z serwera
    };

    if (isLoading && !isEditing) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: '#f4f7f6', color: '#555', fontSize: '18px' }}>
                Ładowanie danych profilu...
            </div>
        );
    }

    const inputStyle = {
        width: '100%', padding: '12px 16px', borderRadius: '12px', border: '1px solid #e2e8f0',
        backgroundColor: isEditing ? '#fff' : '#f7fafc', color: '#2d3748', fontSize: '15px',
        boxSizing: 'border-box' as const, outline: 'none', transition: 'border-color 0.2s', marginTop: '6px'
    };

    const labelStyle = { fontSize: '14px', fontWeight: '600', color: '#4a5568', display: 'block' };

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f4f7f6', padding: '40px 5%', fontFamily: "'Inter', system-ui, sans-serif" }}>
            <div style={{ maxWidth: '800px', margin: '0 auto', backgroundColor: '#fff', borderRadius: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.03)', padding: '40px' }}>

                <div style={{ marginBottom: '30px' }}>
                    <h2 style={{ color: '#2d3748', margin: '0 0 8px 0', fontSize: '28px', fontWeight: '800' }}>Profil Lekarza</h2>
                    <p style={{ color: '#718096', margin: 0, fontSize: '15px' }}>
                        {isEditing ? "Edytuj swoje dane poniżej i kliknij zapisz." : "Poniżej znajdują się Twoje aktualne dane w systemie."}
                    </p>
                </div>

                {error && <div style={{ backgroundColor: '#fed7d7', color: '#c53030', padding: '15px', borderRadius: '12px', marginBottom: '20px', fontWeight: '500' }}>{error}</div>}
                {successMessage && <div style={{ backgroundColor: '#c6f6d5', color: '#2f855a', padding: '15px', borderRadius: '12px', marginBottom: '20px', fontWeight: '500' }}>{successMessage}</div>}

                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '25px' }}>

                    {/* SEKCJA DANYCH OSOBOWYCH */}
                    <div>
                        <h3 style={{ fontSize: '18px', color: '#2d3748', borderBottom: '2px solid #edf2f7', paddingBottom: '10px', marginBottom: '20px' }}>Dane podstawowe</h3>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '20px' }}>
                            <div>
                                <label style={labelStyle}>Imię</label>
                                <input type="text" style={inputStyle} value={firstName} onChange={(e) => setFirstName(e.target.value)} disabled={!isEditing} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Nazwisko</label>
                                <input type="text" style={inputStyle} value={lastName} onChange={(e) => setLastName(e.target.value)} disabled={!isEditing} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Numer telefonu</label>
                                <input type="text" style={inputStyle} value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} disabled={!isEditing} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Numer PWZ (7 cyfr)</label>
                                <input type="text" style={inputStyle} value={licenseNumber} onChange={(e) => setLicenseNumber(e.target.value)} disabled={!isEditing} required />
                            </div>
                        </div>
                    </div>

                    {/* SEKCJA ZMIANY HASŁA (Opcjonalna) */}
                    {isEditing && (
                        <div>
                            <h3 style={{ fontSize: '18px', color: '#2d3748', borderBottom: '2px solid #edf2f7', paddingBottom: '10px', marginBottom: '20px' }}>Zmiana hasła (Opcjonalnie)</h3>
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '20px' }}>
                                <div>
                                    <label style={labelStyle}>Nowe hasło</label>
                                    <input
                                        type="password"
                                        style={inputStyle}
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        placeholder="Pozostaw puste, aby nie zmieniać"
                                    />
                                </div>
                                <div>
                                    <label style={labelStyle}>Powtórz nowe hasło</label>
                                    <input
                                        type="password"
                                        style={inputStyle}
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        placeholder="Powtórz hasło (jeśli zmieniasz)"
                                    />
                                </div>
                            </div>
                        </div>
                    )}

                    {/* SEKCJA SPECJALIZACJI */}
                    <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '2px solid #edf2f7', paddingBottom: '10px', marginBottom: '20px' }}>
                            <h3 style={{ fontSize: '18px', color: '#2d3748', margin: 0 }}>Specjalizacje</h3>
                            {isEditing && (
                                <button type="button" onClick={addSpecialization} style={{ padding: '8px 16px', backgroundColor: '#ebf8ff', color: '#3182ce', border: '1px solid #bee3f8', borderRadius: '10px', fontSize: '14px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s' }}>
                                    + Dodaj specjalizację
                                </button>
                            )}
                        </div>

                        {specializations.map((spec, index) => (
                            <div key={index} style={{ marginBottom: '20px', padding: '20px', backgroundColor: '#f8fafc', borderRadius: '16px', border: '1px solid #e2e8f0', position: 'relative' }}>
                                {isEditing && specializations.length > 1 && (
                                    <button type="button" onClick={() => removeSpecialization(index)} style={{ position: 'absolute', top: '15px', right: '15px', padding: '6px 12px', backgroundColor: '#fed7d7', color: '#c53030', border: 'none', borderRadius: '8px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}>
                                        Usuń
                                    </button>
                                )}
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '15px', color: '#4a5568' }}>{index === 0 ? "Główna Specjalizacja" : `Specjalizacja Dodatkowa ${index}`}</h4>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px' }}>
                                    <div style={{ gridColumn: '1 / -1' }}>
                                        <label style={labelStyle}>Nazwa specjalizacji</label>
                                        <input type="text" style={inputStyle} value={spec.name} onChange={(e) => handleSpecChange(index, 'name', e.target.value)} disabled={!isEditing} placeholder="np. Kardiologia" required />
                                    </div>
                                    <div>
                                        <label style={labelStyle}>Data uzyskania</label>
                                        <input type="date" style={inputStyle} value={spec.obtainedDate} onChange={(e) => handleSpecChange(index, 'obtainedDate', e.target.value)} disabled={!isEditing} required />
                                    </div>
                                    <div>
                                        <label style={labelStyle}>Nr dyplomu/certyfikatu</label>
                                        <input type="text" style={inputStyle} value={spec.certificateNumber} onChange={(e) => handleSpecChange(index, 'certificateNumber', e.target.value)} disabled={!isEditing} required />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* PRZYCISKI AKCJI */}
                    <div style={{ display: 'flex', gap: '15px', marginTop: '15px', flexWrap: 'wrap' }}>
                        {!isEditing ? (
                            <button type="button" onClick={() => setIsEditing(true)} style={{ padding: '14px 24px', backgroundColor: '#3182ce', color: '#fff', border: 'none', borderRadius: '12px', fontSize: '15px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s', flex: 1 }}>
                                Włącz edycję danych
                            </button>
                        ) : (
                            <>
                                <button type="submit" disabled={isLoading} style={{ padding: '14px 24px', backgroundColor: '#38a169', color: '#fff', border: 'none', borderRadius: '12px', fontSize: '15px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s', flex: 1, opacity: isLoading ? 0.7 : 1 }}>
                                    {isLoading ? 'Zapisywanie...' : 'Zapisz zmiany'}
                                </button>
                                <button type="button" onClick={handleCancel} style={{ padding: '14px 24px', backgroundColor: '#e2e8f0', color: '#4a5568', border: 'none', borderRadius: '12px', fontSize: '15px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s', flex: 1 }}>
                                    Anuluj edycję
                                </button>
                            </>
                        )}
                        <button type="button" onClick={() => navigate('/patients')} style={{ padding: '14px 24px', backgroundColor: '#fff', border: '2px solid #e2e8f0', color: '#4a5568', borderRadius: '12px', fontSize: '15px', fontWeight: '600', cursor: 'pointer', transition: 'all 0.2s', flex: 1 }}>
                            Wróć do pacjentów
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default StaffProfile;