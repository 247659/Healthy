import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';

const StaffProfile = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [licenseNumber, setLicenseNumber] = useState('');

    const [specName, setSpecName] = useState('');
    const [specObtainedDate, setSpecObtainedDate] = useState('');
    const [specCertificateNumber, setSpecCertificateNumber] = useState('');

    const [isEditing, setIsEditing] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const token = localStorage.getItem('access_token');
    const API_URL = `http://localhost:8082/api/v1/staff/${id}`;

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

                    {/* SEKCJA SPECJALIZACJI */}
                    <div>
                        <h3 style={{ fontSize: '18px', color: '#2d3748', borderBottom: '2px solid #edf2f7', paddingBottom: '10px', marginBottom: '20px' }}>Specjalizacja Główna</h3>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px' }}>
                            <div style={{ gridColumn: '1 / -1' }}>
                                <label style={labelStyle}>Nazwa specjalizacji</label>
                                <input type="text" style={inputStyle} value={specName} onChange={(e) => setSpecName(e.target.value)} disabled={!isEditing} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Data uzyskania</label>
                                <input type="date" style={inputStyle} value={specObtainedDate} onChange={(e) => setSpecObtainedDate(e.target.value)} disabled={!isEditing} required />
                            </div>
                            <div>
                                <label style={labelStyle}>Nr dyplomu/certyfikatu</label>
                                <input type="text" style={inputStyle} value={specCertificateNumber} onChange={(e) => setSpecCertificateNumber(e.target.value)} disabled={!isEditing} required />
                            </div>
                        </div>
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
                                <button type="button" onClick={() => setIsEditing(false)} style={{ padding: '14px 24px', backgroundColor: '#e2e8f0', color: '#4a5568', border: 'none', borderRadius: '12px', fontSize: '15px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s', flex: 1 }}>
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