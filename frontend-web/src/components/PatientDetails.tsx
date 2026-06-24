import { useState, useEffect, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import type { Patient } from "../types/patient.ts";
import { vitalService } from "../api/vitalClient.ts";
import { api } from "../api/api.ts"; // Zaimportowano axios api do pobrania alertów

interface VitalSigns {
    patientId?: string;
    timestamp: string;
    measurements: {
        heartRate?: number;
        spO2?: number;
        temperature?: number;
        bloodPressure?: {
            systolic?: number;
            diastolic?: number;
        };
    };
}

const PatientDetails = () => {
    const location = useLocation();
    const navigate = useNavigate();

    const patient = location.state?.patient as Patient | undefined;
    const token = localStorage.getItem('access_token');

    const [historyVitals, setHistoryVitals] = useState<VitalSigns[]>([]);
    const [latestVitals, setLatestVitals] = useState<VitalSigns | null>(null);
    const [isVitalsLoading, setIsVitalsLoading] = useState(true);
    const [alerts, setAlerts] = useState<any[]>([]);

    useEffect(() => {
        if (!patient || !token) {
            navigate('/patients');
            return;
        }

        // 1. POBIERANIE HISTORII I ALERTÓW
        const fetchHistoryAndAlerts = async () => {
            setIsVitalsLoading(true);
            try {
                // Dodano pobieranie alertów z użyciem axiosa/Twojego klienta
                const [historyRes, alertsRes] = await Promise.all([
                    vitalService.getHistory(patient.id),
                    api.get(`/notifications/${patient.id}`) // Endpoint z powiadomieniami
                ]);

                // Obsługa historii
                const data = Array.isArray(historyRes) ? historyRes : [];
                setHistoryVitals(data);
                if (data.length > 0) {
                    setLatestVitals(data[0]);
                }

                // Obsługa alertów
                setAlerts(Array.isArray(alertsRes.data) ? alertsRes.data : []);

            } catch (err) {
                console.error("Błąd pobierania danych:", err);
            } finally {
                setIsVitalsLoading(false);
            }
        };

        // 2. POBIERANIE DANYCH LIVE
        const fetchLiveVitals = async () => {
            try {
                const oneMinuteAgo = new Date(Date.now() - 60000).toISOString();
                const res = await vitalService.getHistoryByTime(patient.id, oneMinuteAgo);
                const data = Array.isArray(res) ? res : [];
                if (data.length > 0) {
                    setLatestVitals(data[0]);
                }
            } catch (err) {
                // ignorowane w tle
            }
        };

        // Wywołujemy pobieranie początkowe
        fetchHistoryAndAlerts();

        const intervalId = setInterval(fetchLiveVitals, 5000);

        return () => {
            clearInterval(intervalId);
        };
    }, [patient, token, navigate]);

    if (!patient) return null;

    // --- STATYSTYKI ---
    const getStats = (selector: (v: VitalSigns) => number | undefined) => {
        const values = historyVitals.map(selector).filter(v => v !== undefined && v !== null) as number[];
        if (values.length === 0) return { min: '--', max: '--' };
        return { min: Math.min(...values), max: Math.max(...values) };
    };

    const hrStats = getStats(v => v.measurements?.heartRate);
    const sysStats = getStats(v => v.measurements?.bloodPressure?.systolic);
    const diaStats = getStats(v => v.measurements?.bloodPressure?.diastolic);
    const spo2Stats = getStats(v => v.measurements?.spO2);
    const tempStats = getStats(v => v.measurements?.temperature);

    // --- DANE WYKRESÓW ---
    const chartData = useMemo(() => {
        return [...historyVitals].reverse().map(v => ({
            time: new Date(v.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            heartRate: v.measurements?.heartRate,
            systolic: v.measurements?.bloodPressure?.systolic,
            diastolic: v.measurements?.bloodPressure?.diastolic,
            spO2: v.measurements?.spO2,
            temperature: v.measurements?.temperature
        }));
    }, [historyVitals]);

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f4f7f6', padding: '20px 5%', fontFamily: "'Inter', system-ui, sans-serif" }}>
            <div style={{ maxWidth: '1400px', margin: '0 auto', width: '100%' }}>

                {/* Nagłówek powrotu */}
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: '30px', gap: '20px' }}>
                    <button
                        onClick={() => navigate('/patients')}
                        style={{
                            background: '#fff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '10px 15px',
                            color: '#4a5568', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px',
                            fontSize: '14px', fontWeight: '600', boxShadow: '0 2px 4px rgba(0,0,0,0.02)'
                        }}
                    >
                        <span>&larr;</span> Wróć
                    </button>
                    <h2 style={{ color: '#2d3748', margin: 0, fontSize: '24px', fontWeight: '700' }}>
                        {patient.firstName} {patient.lastName}
                    </h2>
                </div>

                {/* Info i statystyki */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px', marginBottom: '20px' }}>
                    <div style={{ backgroundColor: '#fff', padding: '24px', borderRadius: '20px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
                        <h3 style={{ marginTop: 0, color: '#4a5568', fontSize: '16px', marginBottom: '20px', borderBottom: '1px solid #edf2f7', paddingBottom: '10px' }}>Informacje</h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', fontSize: '15px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>PESEL</span> <strong style={{ color: '#2d3748' }}>{patient.pesel || 'Brak'}</strong></div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>Wiek</span> <strong style={{ color: '#2d3748' }}>{patient.dateOfBirth || 'Brak'}</strong></div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>Telefon</span> <strong style={{ color: '#2d3748' }}>{patient.phoneNumber || 'Brak'}</strong></div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>Adres</span> <strong style={{ color: '#2d3748', textAlign: 'right' }}>{patient.address || 'Brak'}</strong></div>
                        </div>
                    </div>

                    <div style={{ backgroundColor: '#fff', padding: '24px', borderRadius: '20px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
                        <h3 style={{ marginTop: 0, color: '#4a5568', fontSize: '16px', marginBottom: '20px', borderBottom: '1px solid #edf2f7', paddingBottom: '10px' }}>Statystyki z 24h (Min / Max)</h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Tętno (bpm)</span>
                                <strong style={{ color: '#d9534f', fontSize: '16px' }}>{hrStats.min} - {hrStats.max}</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Ciśnienie (mmHg)</span>
                                <strong style={{ color: '#0275d8', fontSize: '16px' }}>{sysStats.min}/{diaStats.min} - {sysStats.max}/{diaStats.max}</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Saturacja (%)</span>
                                <strong style={{ color: '#5cb85c', fontSize: '16px' }}>{spo2Stats.min} - {spo2Stats.max}</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Temperatura (°C)</span>
                                <strong style={{ color: '#ed8936', fontSize: '16px' }}>{tempStats.min} - {tempStats.max}</strong>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Parametry live */}
                <h3 style={{ marginTop: '30px', color: '#2d3748', fontSize: '18px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                    Bieżące parametry życiowe
                    <span style={{ fontSize: '11px', backgroundColor: '#fed7d7', color: '#c53030', padding: '3px 8px', borderRadius: '12px', fontWeight: 'bold' }}>LIVE (odświeżane co 5s)</span>
                </h3>

                {isVitalsLoading ? (
                    <p style={{ color: '#718096' }}>Pobieranie historii z bazy danych...</p>
                ) : latestVitals ? (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px', marginBottom: '40px' }}>
                        <div style={{ backgroundColor: '#fff5f5', padding: '24px', borderRadius: '20px', border: '1px solid #fed7d7', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#c53030', fontWeight: '600', marginBottom: '5px' }}>TĘTNO</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#e53e3e' }}>
                                {latestVitals.measurements?.heartRate ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>bpm</span>
                            </div>
                        </div>

                        <div style={{ backgroundColor: '#ebf8ff', padding: '24px', borderRadius: '20px', border: '1px solid #bee3f8', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#2b6cb0', fontWeight: '600', marginBottom: '5px' }}>CIŚNIENIE KRWI</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#3182ce' }}>
                                {latestVitals.measurements?.bloodPressure?.systolic ?? '--'}<span style={{ fontSize: '28px', color: '#63b3ed' }}>/</span>{latestVitals.measurements?.bloodPressure?.diastolic ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>mmHg</span>
                            </div>
                        </div>

                        <div style={{ backgroundColor: '#f0fff4', padding: '24px', borderRadius: '20px', border: '1px solid #c6f6d5', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#2f855a', fontWeight: '600', marginBottom: '5px' }}>SATURACJA SPO2</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#38a169' }}>
                                {latestVitals.measurements?.spO2 ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>%</span>
                            </div>
                        </div>

                        <div style={{ backgroundColor: '#fffff0', padding: '24px', borderRadius: '20px', border: '1px solid #feebc8', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#c05621', fontWeight: '600', marginBottom: '5px' }}>TEMPERATURA</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#dd6b20' }}>
                                {latestVitals.measurements?.temperature ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>°C</span>
                            </div>
                        </div>
                    </div>
                ) : (
                    <p style={{ color: '#718096' }}>Brak zapisanych pomiarów dla tego pacjenta.</p>
                )}

                {/* Sekcja alertów */}
                <div style={{ backgroundColor: '#fff', padding: '24px', borderRadius: '20px', marginBottom: '20px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
                    <h3 style={{ color: '#e53e3e', fontSize: '18px', marginBottom: '15px' }}>🚨 Historia alertów</h3>
                    {alerts.length > 0 ? (
                        alerts.map((alert, index) => (
                            <div key={index} style={{ padding: '10px', backgroundColor: '#fff5f5', borderRadius: '8px', border: '1px solid #fed7d7', marginBottom: '8px' }}>
                                <p style={{ margin: 0, fontSize: '14px', color: '#822727' }}>
                                    <strong>{alert.timestamp ? new Date(alert.timestamp).toLocaleString() : 'Alert'}:</strong> {alert.message || 'Wykryto nieprawidłowość.'}
                                </p>
                            </div>
                        ))
                    ) : <p style={{ color: '#718096' }}>Brak alertów dla pacjenta.</p>}
                </div>

                {/* WYKRESY */}
                <div style={{ backgroundColor: '#fff', padding: '30px', borderRadius: '24px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
                    <h3 style={{ marginTop: 0, color: '#2d3748', fontSize: '18px', marginBottom: '30px' }}>Historia wykresów (statyczna)</h3>
                    {historyVitals.length > 0 ? (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '50px' }}>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#e53e3e', fontWeight: '600' }}>Wykres Tętna</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={['dataMin - 10', 'dataMax + 10']} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="heartRate" stroke="#e53e3e" strokeWidth={3} dot={{r: 4, fill: '#e53e3e'}} activeDot={{r: 8}} name="Tętno" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#3182ce', fontWeight: '600' }}>Wykres Ciśnienia</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={['auto', 'auto']} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="systolic" stroke="#3182ce" strokeWidth={3} dot={false} name="SYS (Skurczowe)" />
                                        <Line type="monotone" dataKey="diastolic" stroke="#63b3ed" strokeWidth={3} dot={false} name="DIA (Rozkurczowe)" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#38a169', fontWeight: '600' }}>Wykres Saturacji SpO2</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={[80, 100]} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="spO2" stroke="#38a169" strokeWidth={3} dot={{r: 4, fill: '#38a169'}} activeDot={{r: 8}} name="SpO2" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#dd6b20', fontWeight: '600' }}>Wykres Temperatury</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={[35, 42]} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="temperature" stroke="#dd6b20" strokeWidth={3} dot={{r: 4, fill: '#dd6b20'}} activeDot={{r: 8}} name="Temperatura" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                        </div>
                    ) : (
                        <p style={{ color: '#a0aec0' }}>Brak wystarczającej ilości danych do wygenerowania wykresów.</p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PatientDetails;