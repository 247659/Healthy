// frontend/web/src/components/PatientList.tsx
import { useEffect, useState } from 'react';
import axios from 'axios';
import './PatientList.css';

interface Patient {
    id: string;
    firstName: string;
    lastName: string;
    pesel: string;
}

const PatientList = () => {
    const [patients, setPatients] = useState<Patient[]>([]);
    const [error, setError] = useState<string>('');

    useEffect(() => {
        fetchPatients();
    }, []);

    const fetchPatients = async () => {
        const token = localStorage.getItem('access_token');
        if (!token) return;

        try {
            const response = await axios.get('http://localhost:8088/api/v1/patients/allPatients', {
                headers: {
                    Authorization: `Bearer ${token}`
                }
            });
            setPatients(response.data);
        } catch (err) {
            console.error(err);
            setError('Błąd pobierania listy pacjentów. Brak uprawnień lub błąd serwera.');
        }
    };

    return (
        <div className="page-container">
            <div className="page-header">
                <h3 className="page-title">Lista Pacjentów</h3>
            </div>

            {error && <div className="error-message" style={{marginBottom: '20px'}}>{error}</div>}

            <div className="table-card">
                <table className="modern-table">
                    <thead>
                    <tr>
                        <th>ID Pacjenta</th>
                        <th>Imię</th>
                        <th>Nazwisko</th>
                        <th>PESEL</th>
                    </tr>
                    </thead>
                    <tbody>
                    {patients.map(patient => (
                        <tr key={patient.id}>
                            <td><span className="id-badge">{patient.id}</span></td>
                            <td style={{fontWeight: 500}}>{patient.firstName}</td>
                            <td style={{fontWeight: 500}}>{patient.lastName}</td>
                            <td>{patient.pesel}</td>
                        </tr>
                    ))}
                    {patients.length === 0 && !error && (
                        <tr>
                            <td colSpan={4} style={{textAlign: 'center', color: '#6b7280', padding: '30px'}}>
                                Brak pacjentów w bazie danych.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default PatientList;