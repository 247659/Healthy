import { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './Login.css';

const ProfileSetup = () => {
    // --- Dane pobierane automatycznie ---
    const [id, setId] = useState('');

    // --- Dane formularza ---
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [licenseNumber, setLicenseNumber] = useState('');

    // --- Dane specjalizacji ---
    const [specName, setSpecName] = useState('');
    const [specObtainedDate, setSpecObtainedDate] = useState('');
    const [specCertificateNumber, setSpecCertificateNumber] = useState('');

    const [newPassword, setNewPassword] = useState('');

    // ZMIANA: Ustawiamy false jako stan początkowy,
    // ponieważ strona jest gotowa do interakcji od razu.
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    const navigate = useNavigate();

    // 1. Pobranie ID z tokena
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

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const token = localStorage.getItem('access_token');

        try {
            await axios.post('http://localhost:8082/api/v1/staff/save', {
                id: id,
                firstName: firstName,
                lastName: lastName,
                phoneNumber: phoneNumber,
                licenseNumber: licenseNumber,
                specializations: [
                    {
                        name: specName,
                        obtainedDate: specObtainedDate,
                        certificateNumber: specCertificateNumber
                    }
                ]
            }, {
                headers: { Authorization: `Bearer ${token}` }
            });

            navigate('/patients');
        } catch (err: any) {
            console.error('Błąd podczas zapisywania profilu:', err);
            const backendMsg = err.response?.data?.message || 'Sprawdź poprawność danych.';
            setError(`Nie udało się zapisać profilu. ${backendMsg}`);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h2 className="login-title">Dokończ konfigurację</h2>
                <p className="login-subtitle">Uzupełnij swoje dane profilowe</p>

                {error && <div className="error-message">{error}</div>}

                <form className="login-form" onSubmit={handleSubmit}>
                    {/* ... reszta formularza pozostaje bez zmian ... */}
                    <div className="input-group">
                        <label className="input-label">Imię</label>
                        <input type="text" className="login-input" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Nazwisko</label>
                        <input type="text" className="login-input" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Numer telefonu (+48...)</label>
                        <input type="text" className="login-input" value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Numer PWZ (7 cyfr)</label>
                        <input type="text" className="login-input" value={licenseNumber} onChange={(e) => setLicenseNumber(e.target.value)} required />
                    </div>

                    <hr style={{margin: '20px 0', borderTop: '1px solid #ccc'}} />
                    <h3 style={{fontSize: '16px', marginBottom: '10px'}}>Główna Specjalizacja</h3>

                    <div className="input-group">
                        <label className="input-label">Nazwa specjalizacji</label>
                        <input type="text" className="login-input" value={specName} onChange={(e) => setSpecName(e.target.value)} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Data uzyskania</label>
                        <input type="date" className="login-input" value={specObtainedDate} onChange={(e) => setSpecObtainedDate(e.target.value)} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Nr certyfikatu (7 cyfr)</label>
                        <input type="text" className="login-input" value={specCertificateNumber} onChange={(e) => setSpecCertificateNumber(e.target.value)} required />
                    </div>

                    <hr style={{margin: '20px 0', borderTop: '1px solid #ccc'}} />

                    <div className="input-group">
                        <label className="input-label">Ustaw nowe hasło</label>
                        <input type="password" className="login-input" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
                    </div>

                    <button type="submit" className="login-button" disabled={isLoading}>
                        {isLoading ? 'Zapisywanie...' : 'Zapisz i kontynuuj'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ProfileSetup;