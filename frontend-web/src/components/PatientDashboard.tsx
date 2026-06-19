import { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './Login.css';

// Zaktualizowany interfejs na podstawie PatientDto
interface Patient {
    id: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    pesel?: string;
    phoneNumber?: string; // Nowe pole
    address?: string;     // Nowe pole
    dateOfBirth?: string;   // Nowe pole
}

const PatientDashboard = () => {
    const [patients, setPatients] = useState<Patient[]>([]);
    const [assignedPatients, setAssignedPatients] = useState<string[]>([]);
    const [activeTab, setActiveTab] = useState<'assigned' | 'unassigned'>('assigned');

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    const navigate = useNavigate();
    const token = localStorage.getItem('access_token');

    const getDoctorId = () => {
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.sub;
        } catch (e) {
            console.error("Błąd dekodowania tokena", e);
            return null;
        }
    };

    const doctorId = getDoctorId();

    const AGGREGATED_PATIENTS_API_URL = `http://localhost:8080/api/v1/gateway/dashboard/staff/${doctorId}/patients`;
    const STAFF_API_BASE_URL = `http://localhost:8082/api/v1/staff`;

    useEffect(() => {
        if (!token || !doctorId) {
            navigate('/');
            return;
        }

        const fetchData = async () => {
            setIsLoading(true);
            try {
                const [patientsRes, assignedPatientsRes] = await Promise.all([
                    axios.get(`${AGGREGATED_PATIENTS_API_URL}/unassigned`, { headers: { Authorization: `Bearer ${token}` } })
                        .catch(err => {
                            if (err.response && err.response.status === 404) return { data: [] };
                            throw err;
                        }),
                    axios.get(`${AGGREGATED_PATIENTS_API_URL}/assigned`, { headers: {Authorization: `Bearer ${token}`}})
                        .catch(err => {
                            if (err.response && err.response.status === 404) return { data: [] };
                            throw err;
                        })
                ]);

                setPatients(patientsRes.data);
                setAssignedPatients(assignedPatientsRes.data);
            } catch (err) {
                console.error("Błąd pobierania danych:", err);
                setError("Nie udało się pobrać danych pacjentów.");
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, [token, doctorId, navigate]);

    const handleAssignPatient = async (patientId: string) => {
        try {
            await axios.post(`${STAFF_API_BASE_URL}/${doctorId}/assign/${patientId}`, {}, {
                headers: { Authorization: `Bearer ${token}` }
            });

            setAssignedPatients(prev => [...prev, patientId]);
            alert('Pacjent został pomyślnie przypisany!');
        } catch (err) {
            console.error("Błąd przypisywania pacjenta:", err);
            alert("Wystąpił błąd podczas przypisywania pacjenta.");
        }
    };

    if (isLoading) return <div className="login-container">Ładowanie pacjentów...</div>;
    return (
        <div className="login-container" style={{ alignItems: 'flex-start', paddingTop: '50px' }}>
            <div className="login-card" style={{ maxWidth: '800px', width: '100%' }}>
                <h2 className="login-title">Panel Pacjentów</h2>

                {error && <div className="error-message">{error}</div>}

                <div style={{ display: 'flex', marginBottom: '20px', borderBottom: '1px solid #ccc' }}>
                    <button
                        onClick={() => setActiveTab('assigned')}
                        style={{
                            flex: 1, padding: '10px', cursor: 'pointer', border: 'none', background: 'none',
                            borderBottom: activeTab === 'assigned' ? '3px solid #0056b3' : 'none',
                            fontWeight: activeTab === 'assigned' ? 'bold' : 'normal',
                            color: activeTab === 'assigned' ? '#0056b3' : '#666'
                        }}
                    >
                        Moi przypisani pacjenci ({assignedPatients.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('unassigned')}
                        style={{
                            flex: 1, padding: '10px', cursor: 'pointer', border: 'none', background: 'none',
                            borderBottom: activeTab === 'unassigned' ? '3px solid #28a745' : 'none',
                            fontWeight: activeTab === 'unassigned' ? 'bold' : 'normal',
                            color: activeTab === 'unassigned' ? '#28a745' : '#666'
                        }}
                    >
                        Dostępni do przypisania ({patients.length})
                    </button>
                </div>

                {/* Widok 1: Przypisani pacjenci */}
                {activeTab === 'assigned' && (
                    <div>
                        {assignedPatients.length > 0 ? (
                            <ul style={{ listStyle: 'none', padding: 0 }}>
                                {assignedPatients.map(patient => (
                                    <li key={patient.id} style={{ padding: '20px', border: '1px solid #b8daff', backgroundColor: '#f8f9fa', marginBottom: '15px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                                        <strong style={{ color: '#0056b3', fontSize: '18px', display: 'block', borderBottom: '1px solid #dee2e6', paddingBottom: '10px', marginBottom: '10px' }}>
                                            {patient.firstName || 'Brak imienia'} {patient.lastName || 'Brak nazwiska'}
                                        </strong>
                                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '14px', color: '#444' }}>
                                            <div><strong>PESEL:</strong> {patient.pesel || 'Brak'}</div>
                                            <div><strong>Data ur.:</strong> {patient.dateOfBirth || 'Brak'}</div>
                                            <div><strong>Telefon:</strong> {patient.phoneNumber || 'Brak'}</div>
                                            <div><strong>Email:</strong> {patient.email || 'Brak'}</div>
                                            <div style={{ gridColumn: '1 / span 2', marginTop: '5px', paddingTop: '5px', borderTop: '1px dashed #e9ecef' }}>
                                                <strong>Adres:</strong> {patient.address || 'Brak'}
                                            </div>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p style={{ textAlign: 'center', color: '#666', padding: '20px' }}>Nie masz jeszcze przypisanych żadnych pacjentów.</p>
                        )}
                    </div>
                )}

                {/* Widok 2: Pacjenci dostępni do przypisania */}
                {activeTab === 'unassigned' && (
                    <div>
                        {patients.length > 0 ? (
                            <ul style={{ listStyle: 'none', padding: 0 }}>
                                {patients.map(patient => (
                                    <li key={patient.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '20px', border: '1px solid #eee', marginBottom: '15px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                                        <div style={{ flex: 1 }}>
                                            <strong style={{ fontSize: '18px', color: '#333', display: 'block', borderBottom: '1px solid #eee', paddingBottom: '10px', marginBottom: '10px' }}>
                                                {patient.firstName || 'Brak imienia'} {patient.lastName || 'Brak nazwiska'}
                                            </strong>
                                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '14px', color: '#555' }}>
                                                <div><strong>PESEL:</strong> {patient.pesel || 'Brak'}</div>
                                                <div><strong>Data ur.:</strong> {patient.dateOfBirth || 'Brak'}</div>
                                                <div><strong>Telefon:</strong> {patient.phoneNumber || 'Brak'}</div>
                                                <div><strong>Email:</strong> {patient.email || 'Brak'}</div>
                                                <div style={{ gridColumn: '1 / span 2', marginTop: '5px', paddingTop: '5px', borderTop: '1px dashed #eee' }}>
                                                    <strong>Adres:</strong> {patient.address || 'Brak'}
                                                </div>
                                            </div>
                                        </div>
                                        <div style={{ marginLeft: '20px' }}>
                                            <button
                                                className="login-button"
                                                style={{ width: 'auto', padding: '10px 20px', marginTop: 0, backgroundColor: '#28a745' }}
                                                onClick={() => handleAssignPatient(patient.id)}
                                            >
                                                + Przypisz do mnie
                                            </button>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p style={{ textAlign: 'center', color: '#666', padding: '20px' }}>Wszyscy pacjenci w systemie są już do Ciebie przypisani lub brak pacjentów w ogóle!</p>
                        )}
                    </div>
                )}

            </div>
        </div>
    );
};

export default PatientDashboard;