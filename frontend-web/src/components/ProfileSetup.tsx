import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './Login.css'; // Korzystamy z tych samych klas co w oknie logowania dla zachowania spójności

const ProfileSetup = () => {
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [specialization, setSpecialization] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const token = localStorage.getItem('access_token');

        try {
            // 1. Zapisanie danych profilowych (Upewnij się, że endpoint i struktura DTO odpowiadają Twojemu API)
            await axios.post('http://localhost:8082/api/v1/staff', {
                firstName: firstName,
                lastName: lastName,
                specialty: specialization // dopasuj pole w zależności od backendu (np. 'specialization' czy 'specialty')
            }, {
                headers: { Authorization: `Bearer ${token}` }
            });

            // 2. Jeśli podano nowe hasło, wyślij je do serwisu autoryzacji
            if (newPassword) {
                // Przykład: axios.post('http://localhost:8080/api/v1/auth/change-password', ...)
                // Uzupełnij właściwym adresem jeśli udostępniasz taki endpoint
            }

            // Pomyślnie zaktualizowano -> kierujemy do aplikacji
            navigate('/patients');
        } catch (err: any) {
            console.error('Błąd podczas zapisywania profilu:', err);
            setError('Nie udało się zapisać profilu. Upewnij się, że wszystkie dane są poprawne.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h2 className="login-title">Dokończ konfigurację konta</h2>
                <p className="login-subtitle">Uzupełnij swoje dane profilowe i ustaw docelowe hasło</p>

                {error && <div className="error-message">{error}</div>}

                <form className="login-form" onSubmit={handleSubmit}>
                    <div className="input-group">
                        <label className="input-label">Imię</label>
                        <input
                            type="text"
                            className="login-input"
                            value={firstName}
                            onChange={(e) => setFirstName(e.target.value)}
                            placeholder="np. Jan"
                            required
                            disabled={isLoading}
                        />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Nazwisko</label>
                        <input
                            type="text"
                            className="login-input"
                            value={lastName}
                            onChange={(e) => setLastName(e.target.value)}
                            placeholder="np. Kowalski"
                            required
                            disabled={isLoading}
                        />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Specjalizacja</label>
                        <input
                            type="text"
                            className="login-input"
                            value={specialization}
                            onChange={(e) => setSpecialization(e.target.value)}
                            placeholder="np. Kardiolog"
                            required
                            disabled={isLoading}
                        />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Ustaw nowe hasło (wymagane)</label>
                        <input
                            type="password"
                            className="login-input"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            placeholder="Min. 8 znaków"
                            required
                            disabled={isLoading}
                        />
                    </div>

                    <button
                        type="submit"
                        className="login-button"
                        disabled={isLoading}
                        style={{
                            opacity: isLoading ? 0.7 : 1,
                            cursor: isLoading ? 'wait' : 'pointer',
                            marginTop: '10px'
                        }}
                    >
                        {isLoading ? 'Zapisywanie...' : 'Zapisz i kontynuuj'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ProfileSetup;