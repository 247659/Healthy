import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {medicalStaffService} from "../api/staffClient.ts";
import {gatewayService} from "../api/gatewayClient.ts";
import type {Patient} from "../types/patient.ts";

const PatientDashboard = () => {
    const [patients, setPatients] = useState<Patient[]>([]);
    const [assignedPatients, setAssignedPatients] = useState<Patient[]>([]);
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

    useEffect(() => {
        if (!token || !doctorId) {
            navigate('/');
            return;
        }

        const fetchData = async () => {
            setIsLoading(true);
            try {
                const [patientsRes, assignedPatientsRes] = await Promise.all([
                    await gatewayService.getUnassignedPatients(doctorId),
                    await gatewayService.getAssignedPatients(doctorId)
                ]);

                setPatients(Array.isArray(patientsRes) ? patientsRes : []);
                setAssignedPatients(Array.isArray(assignedPatientsRes) ? assignedPatientsRes : []);
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
            await medicalStaffService.assignPatient(doctorId, patientId);

            const patientToAdd = patients.find(p => p.id === patientId);
            if (patientToAdd) {
                setAssignedPatients(prev => [...prev, patientToAdd]);
                setPatients(prev => prev.filter(p => p.id !== patientId));
            }
        } catch (err) {
            console.error("Błąd przypisywania pacjenta:", err);
            alert("Wystąpił błąd podczas przypisywania pacjenta.");
        }
    };

    const handleOpenPatientCard = (patient: Patient) => {
        navigate(`/patient/${patient.id}`, { state: { patient } });
    };

    const handleUnassignPatient = async (patientId: string) => {
        try {
            // Zakładam, że taka metoda istnieje w Twoim API
            await medicalStaffService.unassignPatient(doctorId, patientId);

            const patientToRemove = assignedPatients.find(p => p.id === patientId);
            if (patientToRemove) {
                setPatients(prev => [...prev, patientToRemove]);
                setAssignedPatients(prev => prev.filter(p => p.id !== patientId));
            }
        } catch (err) {
            console.error("Błąd odpinania pacjenta:", err);
            alert("Wystąpił błąd podczas odpinania pacjenta.");
        }
    };

    if (isLoading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: '#f4f7f6', color: '#555', fontSize: '18px' }}>
                Ładowanie pacjentów...
            </div>
        );
    }

    const renderPatientCard = (patient: Patient, isAssigned: boolean) => (
        <div key={patient.id} style={{
            backgroundColor: '#fff',
            borderRadius: '16px',
            padding: '20px',
            marginBottom: '15px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
            display: 'flex',
            flexDirection: 'column',
            gap: '15px',
            border: '1px solid #edf2f7'
        }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #edf2f7', paddingBottom: '15px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                    <div style={{ width: '48px', height: '48px', borderRadius: '50%', backgroundColor: isAssigned ? '#e3f2fd' : '#f0f4f8', color: isAssigned ? '#1976d2' : '#64748b', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px', fontWeight: 'bold' }}>
                        {patient.firstName?.charAt(0) || ''}{patient.lastName?.charAt(0) || ''}
                    </div>
                    <div>
                        <strong style={{ color: '#2d3748', fontSize: '18px', display: 'block' }}>
                            {patient.firstName || 'Brak imienia'} {patient.lastName || 'Brak nazwiska'}
                        </strong>
                        <span style={{ fontSize: '13px', color: '#718096' }}>PESEL: {patient.pesel || 'Brak'}</span>
                    </div>
                </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '10px', fontSize: '14px', color: '#4a5568' }}>
                <div><span style={{ color: '#a0aec0', fontSize: '12px', display: 'block' }}>Data urodzenia</span> {patient.dateOfBirth || 'Brak'}</div>
                <div><span style={{ color: '#a0aec0', fontSize: '12px', display: 'block' }}>Telefon</span> {patient.phoneNumber || 'Brak'}</div>
                <div><span style={{ color: '#a0aec0', fontSize: '12px', display: 'block' }}>Email</span> {patient.email || 'Brak'}</div>
            </div>

            <div style={{ paddingTop: '10px', display: 'flex', gap: '10px' }}>
                {isAssigned ? (
                    <>
                        <button
                            style={{ flex: 1, padding: '12px', backgroundColor: '#3182ce', color: '#fff', border: 'none', borderRadius: '12px', fontSize: '14px', cursor: 'pointer' }}
                            onClick={() => handleOpenPatientCard(patient)}
                        >
                            Karta
                        </button>
                        <button
                            style={{ flex: 1, padding: '12px', backgroundColor: '#e53e3e', color: '#fff', border: 'none', borderRadius: '12px', fontSize: '14px', cursor: 'pointer' }}
                            onClick={() => handleUnassignPatient(patient.id)}
                            onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#c53030'}
                            onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#e53e3e'}
                        >
                            Odepnij
                        </button>
                    </>
                ) : (
                    <button
                        style={{ padding: '12px 24px', backgroundColor: '#38a169', color: '#fff', border: 'none', borderRadius: '12px', fontSize: '14px', fontWeight: '600', cursor: 'pointer', width: '100%' }}
                        onClick={() => handleAssignPatient(patient.id)}
                    >
                        + Przypisz do mnie
                    </button>
                )}
            </div>
        </div>
    );

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f4f7f6', padding: '20px 5%', fontFamily: "'Inter', system-ui, sans-serif" }}>
            <div style={{ maxWidth: '1400px', margin: '0 auto', width: '100%' }}>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px', flexWrap: 'wrap', gap: '20px' }}>
                    <h2 style={{ color: '#2d3748', margin: 0, fontSize: '28px', fontWeight: '700' }}>Panel Pacjentów</h2>
                </div>

                {error && <div style={{ backgroundColor: '#fed7d7', color: '#c53030', padding: '15px', borderRadius: '12px', marginBottom: '20px' }}>{error}</div>}

                {/* Nowoczesne zakładki - DODANO margin: '0 auto' ORAZ width: '100%' */}
                <div style={{ display: 'flex', backgroundColor: '#e2e8f0', borderRadius: '16px', padding: '6px', margin: '0 auto 30px auto', maxWidth: '600px', width: '100%' }}>
                    <button
                        onClick={() => setActiveTab('assigned')}
                        style={{
                            flex: 1, padding: '12px 20px', cursor: 'pointer', border: 'none',
                            backgroundColor: activeTab === 'assigned' ? '#fff' : 'transparent',
                            borderRadius: '12px',
                            fontWeight: activeTab === 'assigned' ? '600' : '500',
                            color: activeTab === 'assigned' ? '#2b6cb0' : '#718096',
                            boxShadow: activeTab === 'assigned' ? '0 2px 4px rgba(0,0,0,0.05)' : 'none',
                            transition: 'all 0.3s ease'
                        }}
                    >
                        Moi pacjenci ({assignedPatients.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('unassigned')}
                        style={{
                            flex: 1, padding: '12px 20px', cursor: 'pointer', border: 'none',
                            backgroundColor: activeTab === 'unassigned' ? '#fff' : 'transparent',
                            borderRadius: '12px',
                            fontWeight: activeTab === 'unassigned' ? '600' : '500',
                            color: activeTab === 'unassigned' ? '#2f855a' : '#718096',
                            boxShadow: activeTab === 'unassigned' ? '0 2px 4px rgba(0,0,0,0.05)' : 'none',
                            transition: 'all 0.3s ease'
                        }}
                    >
                        Dostępni ({patients.length})
                    </button>
                </div>

                {/* Siatka pacjentów */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '20px' }}>
                    {activeTab === 'assigned' && (
                        assignedPatients.length > 0
                            ? assignedPatients.map(p => renderPatientCard(p, true))
                            : <p style={{ color: '#a0aec0', fontSize: '16px', gridColumn: '1 / -1', textAlign: 'center', padding: '40px' }}>Nie masz przypisanych pacjentów.</p>
                    )}

                    {activeTab === 'unassigned' && (
                        patients.length > 0
                            ? patients.map(p => renderPatientCard(p, false))
                            : <p style={{ color: '#a0aec0', fontSize: '16px', gridColumn: '1 / -1', textAlign: 'center', padding: '40px' }}>Brak nowych pacjentów do przypisania.</p>
                    )}
                </div>

            </div>
        </div>
    );
};

export default PatientDashboard;