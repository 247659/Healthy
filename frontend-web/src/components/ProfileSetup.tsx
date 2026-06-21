import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { MedicalStaff } from "../types/medicalStaff.ts";
import { medicalStaffService } from "../api/staffClient.ts";

// Pomocniczy interfejs dla stanu specjalizacji
interface SpecState {
    name: string;
    obtainedDate: string;
    certificateNumber: string;
}

const ProfileSetup = () => {
    const [id, setId] = useState('');

    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [licenseNumber, setLicenseNumber] = useState('');

    // Stan tablicowy dla wielu specjalizacji
    const [specializations, setSpecializations] = useState<SpecState[]>([
        { name: '', obtainedDate: '', certificateNumber: '' }
    ]);

    const [newPassword, setNewPassword] = useState('');
    // Stan do weryfikacji powtórzonego hasła
    const [confirmPassword, setConfirmPassword] = useState('');

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem('access_token');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                setId(payload.sub);
            } catch (e) {
                console.error("Błąd dekodowania tokena", e);
                setError("Błąd sesji. Zaloguj się ponownie.");
            }
        }
    }, []);

    // Funkcje do obsługi dynamicznej listy specjalizacji
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

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        // Weryfikacja powtórzenia hasła
        if (newPassword !== confirmPassword) {
            setError('Podane hasła nie są identyczne. Spróbuj ponownie.');
            setIsLoading(false);
            return;
        }

        try {
            const data: MedicalStaff = {
                id: id,
                password: newPassword, // Wysyłamy hasło do aktualizacji w Keycloak
                firstName: firstName,
                lastName: lastName,
                phoneNumber: phoneNumber,
                licenseNumber: licenseNumber,
                specializations: specializations // Wysyłamy listę specjalizacji
            }
            await medicalStaffService.setupProfile(data)

            navigate('/patients');
        } catch (err: any) {
            console.error('Błąd podczas zapisywania profilu:', err);
            const backendMsg = err.response?.data?.message || 'Sprawdź poprawność danych (np. 7 cyfr dla numeru PWZ).';
            setError(`Nie udało się zapisać profilu. ${backendMsg}`);
        } finally {
            setIsLoading(false);
        }
    };

    const inputStyle = {
        width: '100%', padding: '12px 16px', borderRadius: '12px', border: '1px solid #e2e8f0',
        backgroundColor: '#fff', color: '#2d3748', fontSize: '15px',
        boxSizing: 'border-box' as const, outline: 'none', transition: 'border-color 0.2s', marginTop: '6px'
    };

    const labelStyle = { fontSize: '14px', fontWeight: '600', color: '#4a5568', display: 'block' };

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f4f7f6', padding: '40px 5%', fontFamily: "'Inter', system-ui, sans-serif" }}>
            <div style={{ maxWidth: '800px', margin: '0 auto', backgroundColor: '#fff', borderRadius: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.03)', padding: '40px' }}>

                <div style={{ marginBottom: '30px', textAlign: 'center' }}>
                    <div style={{ fontSize: '40px', marginBottom: '10px' }}>👋</div>
                    <h2 style={{ color: '#2d3748', margin: '0 0 8px 0', fontSize: '28px', fontWeight: '800' }}>Witaj w HealthMonitor</h2>
                    <p style={{ color: '#718096', margin: 0, fontSize: '15px' }}>Zanim zaczniesz, uzupełnij swoje dane profilowe.</p>
                </div>

                {error && <div style={{ backgroundColor: '#fed7d7', color: '#c53030', padding: '15px', borderRadius: '12px', marginBottom: '20px', fontWeight: '500' }}>{error}</div>}

                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '25px' }}>

                    {/* DANE OSOBOWE */}
                    <div style={{ padding: '20px', backgroundColor: '#f8fafc', borderRadius: '16px', border: '1px solid #edf2f7' }}>
                        <h3 style={{ fontSize: '16px', color: '#2d3748', margin: '0 0 20px 0' }}>Krok 1: Dane podstawowe</h3>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px' }}>
                            <div>
                                <label style={labelStyle}>Imię</label>
                                <input type="text" style={inputStyle} value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Nazwisko</label>
                                <input type="text" style={inputStyle} value={lastName} onChange={(e) => setLastName(e.target.value)} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Telefon (+48)</label>
                                <input type="text" style={inputStyle} value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Nr PWZ (7 cyfr)</label>
                                <input type="text" style={inputStyle} value={licenseNumber} onChange={(e) => setLicenseNumber(e.target.value)} required />
                            </div>
                        </div>
                    </div>

                    {/* SPECJALIZACJA */}
                    <div style={{ padding: '20px', backgroundColor: '#f8fafc', borderRadius: '16px', border: '1px solid #edf2f7' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h3 style={{ fontSize: '16px', color: '#2d3748', margin: 0 }}>Krok 2: Specjalizacje</h3>
                            <button type="button" onClick={addSpecialization} style={{ padding: '6px 12px', backgroundColor: '#ebf8ff', color: '#3182ce', border: '1px solid #bee3f8', borderRadius: '8px', fontSize: '13px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s' }}>
                                + Dodaj kolejną
                            </button>
                        </div>

                        {specializations.map((spec, index) => (
                            <div key={index} style={{ marginBottom: index !== specializations.length - 1 ? '20px' : '0', paddingBottom: index !== specializations.length - 1 ? '20px' : '0', borderBottom: index !== specializations.length - 1 ? '1px solid #e2e8f0' : 'none', position: 'relative' }}>
                                {specializations.length > 1 && (
                                    <button type="button" onClick={() => removeSpecialization(index)} style={{ position: 'absolute', top: '-5px', right: '0', padding: '4px 8px', backgroundColor: '#fed7d7', color: '#c53030', border: 'none', borderRadius: '6px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}>
                                        Usuń
                                    </button>
                                )}
                                <h4 style={{ margin: '0 0 12px 0', fontSize: '14px', color: '#4a5568' }}>{index === 0 ? "Główna Specjalizacja" : `Dodatkowa Specjalizacja ${index}`}</h4>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px' }}>
                                    <div style={{ gridColumn: '1 / -1' }}>
                                        <label style={labelStyle}>Nazwa specjalizacji</label>
                                        <input type="text" style={inputStyle} value={spec.name} onChange={(e) => handleSpecChange(index, 'name', e.target.value)} placeholder="np. Kardiologia" required />
                                    </div>
                                    <div>
                                        <label style={labelStyle}>Data uzyskania</label>
                                        <input type="date" style={inputStyle} value={spec.obtainedDate} onChange={(e) => handleSpecChange(index, 'obtainedDate', e.target.value)} required />
                                    </div>
                                    <div>
                                        <label style={labelStyle}>Nr certyfikatu (7 cyfr)</label>
                                        <input type="text" style={inputStyle} value={spec.certificateNumber} onChange={(e) => handleSpecChange(index, 'certificateNumber', e.target.value)} required />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* BEZPIECZEŃSTWO */}
                    <div style={{ padding: '20px', backgroundColor: '#fff5f5', borderRadius: '16px', border: '1px solid #fed7d7' }}>
                        <h3 style={{ fontSize: '16px', color: '#c53030', margin: '0 0 15px 0' }}>Krok 3: Bezpieczeństwo konta</h3>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px' }}>
                            <div>
                                <label style={{ ...labelStyle, color: '#9b2c2c' }}>Ustaw nowe bezpieczne hasło</label>
                                <input type="password" style={{ ...inputStyle, borderColor: '#feb2b2' }} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="Minimum 8 znaków" required />
                            </div>
                            <div>
                                <label style={{ ...labelStyle, color: '#9b2c2c' }}>Powtórz nowe hasło</label>
                                <input type="password" style={{ ...inputStyle, borderColor: '#feb2b2' }} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} placeholder="Powtórz hasło" required />
                            </div>
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={isLoading}
                        style={{
                            padding: '16px 24px', backgroundColor: '#3182ce', color: '#fff', border: 'none',
                            borderRadius: '12px', fontSize: '16px', fontWeight: '700', cursor: 'pointer',
                            transition: 'background-color 0.2s', width: '100%', marginTop: '10px',
                            opacity: isLoading ? 0.7 : 1
                        }}
                    >
                        {isLoading ? 'Konfiguracja...' : 'Zapisz profil i przejdź do panelu 🚀'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ProfileSetup;