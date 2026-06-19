import { useState, useEffect } from 'react';
import axios from 'axios';
import { useLocation, useNavigate } from 'react-router-dom';

export interface Patient {
    id: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    pesel?: string;
    phoneNumber?: string;
    address?: string;
    dateOfBirth?: string;
}

interface VitalSigns {
    id?: string;
    patientId?: string;
    timestamp: string;
    heartRate?: number;
    bloodPressureSystolic?: number;
    bloodPressureDiastolic?: number;
    oxygenSaturation?: number;
    temperature?: number;
}

const PatientDetails = () => {
    const location = useLocation();
    const navigate = useNavigate();

    // Odczytujemy obiekt pacjenta, który przekażemy podczas nawigacji
    const patient = location.state?.patient as Patient | undefined;
    const token = localStorage.getItem('access_token');

    const [vitals, setVitals] = useState<VitalSigns[]>([]);
    const [isVitalsLoading, setIsVitalsLoading] = useState(false);

    const VITALS_API_BASE_URL = `http://localhost:8080/vital-signs/patient`;

    useEffect(() => {
        // Jeśli wejdziemy bezpośrednio z linku i nie mamy danych pacjenta, wracamy do listy
        if (!patient || !token) {
            navigate('/patients');
            return;
        }

        const fetchVitals = async (isFirstLoad: boolean) => {
            if (isFirstLoad) {
                setIsVitalsLoading(true);
            }

            try {
                const res = await axios.get(`${VITALS_API_BASE_URL}/${patient.id}`, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                setVitals(Array.isArray(res.data) ? res.data : []);
            } catch (err) {
                console.error("Błąd pobierania historii pomiarów:", err);
            } finally {
                setIsVitalsLoading(false);
            }
        };

        fetchVitals(true);

        const intervalId = setInterval(() => {
            fetchVitals(false);
        }, 5000);

        return () => {
            clearInterval(intervalId);
        };
    }, [patient, token, navigate]);

    // Ochrona przed renderowaniem, gdy brakuje pacjenta (przed redirectem)
    if (!patient) return null;

    const latestVitals = vitals.length > 0 ? vitals[0] : null;

    return (
        <div className="login-container" style={{ alignItems: 'flex-start', paddingTop: '50px' }}>
            <div className="login-card" style={{ maxWidth: '800px', width: '100%' }}>
                <button
                    onClick={() => navigate('/patients')} // Cofa nas na stronę główną pacjentów
                    style={{ background: 'none', border: 'none', color: '#0056b3', cursor: 'pointer', marginBottom: '20px', fontSize: '16px', fontWeight: 'bold' }}
                >
                    &larr; Wróć do listy pacjentów
                </button>

                <h2 className="login-title">Karta pacjenta: {patient.firstName} {patient.lastName}</h2>

                <div style={{ padding: '20px', border: '1px solid #dee2e6', borderRadius: '8px', marginBottom: '20px', backgroundColor: '#f8f9fa' }}>
                    <h3 style={{ marginTop: 0, color: '#333' }}>Dane podstawowe</h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '15px' }}>
                        <div><strong>PESEL:</strong> {patient.pesel || 'Brak'}</div>
                        <div><strong>Data ur.:</strong> {patient.dateOfBirth || 'Brak'}</div>
                        <div><strong>Telefon:</strong> {patient.phoneNumber || 'Brak'}</div>
                        <div><strong>Email:</strong> {patient.email || 'Brak'}</div>
                        <div style={{ gridColumn: '1 / span 2' }}><strong>Adres:</strong> {patient.address || 'Brak'}</div>
                    </div>
                </div>

                <div style={{ padding: '20px', border: '1px solid #b8daff', borderRadius: '8px', marginBottom: '20px', backgroundColor: '#e9f7ef' }}>
                    <h3 style={{ marginTop: 0, color: '#28a745', display: 'flex', justifyContent: 'space-between' }}>
                        Aktualne pomiary (Na żywo)
                        <span style={{ fontSize: '12px', fontWeight: 'normal', color: '#666', animation: 'pulse 1.5s infinite' }}>🔴 Live</span>
                    </h3>
                    {isVitalsLoading && vitals.length === 0 ? (
                        <p>Ładowanie pierwszych danych...</p>
                    ) : latestVitals ? (
                        <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                            <div style={{ flex: 1, padding: '15px', background: '#fff', borderRadius: '8px', textAlign: 'center', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                                <div style={{ fontSize: '14px', color: '#666' }}>Tętno</div>
                                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#d9534f' }}>{latestVitals.heartRate ?? '--'} <small>bpm</small></div>
                            </div>
                            <div style={{ flex: 1, padding: '15px', background: '#fff', borderRadius: '8px', textAlign: 'center', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                                <div style={{ fontSize: '14px', color: '#666' }}>Ciśnienie</div>
                                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#0275d8' }}>{latestVitals.bloodPressureSystolic ?? '--'} / {latestVitals.bloodPressureDiastolic ?? '--'}</div>
                            </div>
                            <div style={{ flex: 1, padding: '15px', background: '#fff', borderRadius: '8px', textAlign: 'center', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                                <div style={{ fontSize: '14px', color: '#666' }}>Saturacja (SpO2)</div>
                                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#5cb85c' }}>{latestVitals.oxygenSaturation ?? '--'} <small>%</small></div>
                            </div>
                            <div style={{ width: '100%', textAlign: 'right', fontSize: '12px', color: '#999', marginTop: '10px' }}>
                                Ostatnia aktualizacja: {new Date(latestVitals.timestamp).toLocaleString()}
                            </div>
                        </div>
                    ) : (
                        <p>Brak aktualnych pomiarów dla tego pacjenta.</p>
                    )}
                </div>

                <div style={{ padding: '20px', border: '1px solid #dee2e6', borderRadius: '8px' }}>
                    <h3 style={{ marginTop: 0, color: '#333' }}>Historia pomiarów (24h)</h3>
                    {vitals.length > 0 ? (
                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
                            <thead>
                            <tr style={{ borderBottom: '2px solid #ddd' }}>
                                <th style={{ padding: '10px' }}>Czas</th>
                                <th style={{ padding: '10px' }}>Tętno</th>
                                <th style={{ padding: '10px' }}>Ciśnienie</th>
                                <th style={{ padding: '10px' }}>SpO2</th>
                            </tr>
                            </thead>
                            <tbody>
                            {vitals.map((v, idx) => (
                                <tr key={idx} style={{ borderBottom: '1px solid #eee' }}>
                                    <td style={{ padding: '10px' }}>{new Date(v.timestamp).toLocaleString()}</td>
                                    <td style={{ padding: '10px' }}>{v.heartRate} bpm</td>
                                    <td style={{ padding: '10px' }}>{v.bloodPressureSystolic}/{v.bloodPressureDiastolic}</td>
                                    <td style={{ padding: '10px' }}>{v.oxygenSaturation}%</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    ) : (
                        <p>Brak historii pomiarów z ostatnich 24 godzin.</p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PatientDetails;