import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';
import './Login.css'; // Używamy Twoich stylów dla spójności

const StaffProfile = () => {
    // Pobieramy ID lekarza z adresu URL (np. /profile/123e4567-e89b-12d3-a456-426614174000)
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    // Stany dla danych formularza
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [licenseNumber, setLicenseNumber] = useState('');

    // Stany dla specjalizacji
    const [specName, setSpecName] = useState('');
    const [specObtainedDate, setSpecObtainedDate] = useState('');
    const [specCertificateNumber, setSpecCertificateNumber] = useState('');

    // Stany widoku
    const [isEditing, setIsEditing] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const token = localStorage.getItem('access_token');
    const API_URL = `http://localhost:8082/api/v1/staff/${id}`;

    // Pobieranie danych przy załadowaniu komponentu
    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const response = await axios.get(API_URL, {
                    headers: { Authorization: `Bearer ${token}` }
                });

                const data = response.data;
                setFirstName(data.firstName);
                setLastName(data.lastName);
                setPhoneNumber(data.phoneNumber);
                setLicenseNumber(data.licenseNumber);

                if (data.specializations && data.specializations.length > 0) {
                    const spec = data.specializations[0];
                    setSpecName(spec.name);
                    setSpecObtainedDate(spec.obtainedDate);
                    setSpecCertificateNumber(spec.certificateNumber);
                }
                setIsLoading(false);
            } catch (err) {
                console.error("Błąd pobierania profilu:", err);
                setError("Nie udało się załadować danych profilu.");
                setIsLoading(false);
            }
        };

        if (id) {
            fetchProfile();
        }
    }, [id, API_URL, token]);

    // Obsługa zapisu (Aktualizacja danych)
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');
        setIsLoading(true);

        try {
            await axios.put(API_URL, {
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

            setSuccessMessage('Profil został pomyślnie zaktualizowany!');
            setIsEditing(false);
        } catch (err: any) {
            console.error('Błąd aktualizacji:', err);
            const backendMsg = err.response?.data?.message || 'Sprawdź poprawność danych (np. 7 cyfr dla numerów).';
            setError(`Nie udało się zaktualizować profilu. ${backendMsg}`);
        } finally {
            setIsLoading(false);
        }
    };

    if (isLoading && !isEditing) return <div className="login-container">Ładowanie danych...</div>;

    return (
        <div className="login-container">
            <div className="login-card" style={{ maxWidth: '600px' }}>
                <h2 className="login-title">Profil Lekarza</h2>
                <p className="login-subtitle">
                    {isEditing ? "Edytuj swoje dane poniżej" : "Twoje aktualne dane w systemie"}
                </p>

                {error && <div className="error-message">{error}</div>}
                {successMessage && <div style={{ color: 'green', marginBottom: '15px', textAlign: 'center' }}>{successMessage}</div>}

                <form className="login-form" onSubmit={handleSubmit}>
                    <div className="input-group">
                        <label className="input-label">Imię</label>
                        <input type="text" className="login-input" value={firstName} onChange={(e) => setFirstName(e.target.value)} disabled={!isEditing} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Nazwisko</label>
                        <input type="text" className="login-input" value={lastName} onChange={(e) => setLastName(e.target.value)} disabled={!isEditing} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Numer telefonu</label>
                        <input type="text" className="login-input" value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} disabled={!isEditing} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Numer PWZ (7 cyfr)</label>
                        <input type="text" className="login-input" value={licenseNumber} onChange={(e) => setLicenseNumber(e.target.value)} disabled={!isEditing} required />
                    </div>

                    <hr style={{margin: '20px 0', borderTop: '1px solid #ccc'}} />
                    <h3 style={{fontSize: '16px', marginBottom: '10px'}}>Specjalizacja</h3>

                    <div className="input-group">
                        <label className="input-label">Nazwa specjalizacji</label>
                        <input type="text" className="login-input" value={specName} onChange={(e) => setSpecName(e.target.value)} disabled={!isEditing} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Data uzyskania</label>
                        <input type="date" className="login-input" value={specObtainedDate} onChange={(e) => setSpecObtainedDate(e.target.value)} disabled={!isEditing} required />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Nr dyplomu (7 cyfr)</label>
                        {/* TUTAJ BYŁ BŁĄD: Zmieniono na setSpecCertificateNumber */}
                        <input type="text" className="login-input" value={specCertificateNumber} onChange={(e) => setSpecCertificateNumber(e.target.value)} disabled={!isEditing} required />
                    </div>

                    <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                        {!isEditing ? (
                            <button type="button" className="login-button" onClick={() => setIsEditing(true)}>
                                Edytuj profil
                            </button>
                        ) : (
                            <>
                                <button type="submit" className="login-button" disabled={isLoading}>
                                    {isLoading ? 'Zapisywanie...' : 'Zapisz zmiany'}
                                </button>
                                <button type="button" className="login-button" style={{ backgroundColor: '#ccc', color: '#333' }} onClick={() => setIsEditing(false)}>
                                    Anuluj
                                </button>
                            </>
                        )}
                        <button type="button" className="login-button" style={{ backgroundColor: '#f0ad4e' }} onClick={() => navigate('/patients')}>
                            Wróć do listy pacjentów
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default StaffProfile;