// frontend/web/src/components/Login.tsx
import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './Login.css';
import {authService} from "../api/authClient.ts";

// Funkcja pomocnicza do dekodowania tokenu JWT (nie wymaga instalacji zewnętrznych bibliotek)
const getUserIdFromToken = (token: string) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        const decoded = JSON.parse(jsonPayload);
        return decoded.sub; // W Keycloak pole 'sub' to UUID użytkownika
    } catch (e) {
        console.error("Błąd dekodowania tokenu", e);
        return null;
    }
};

const Login = ({ setToken, setRefreshToken }: { setToken: (token: string) => void, setRefreshToken: (refreshToken: string) => void }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const navigate = useNavigate();

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const response = await authService.login({ email, password });

            const accessToken = response.accessToken
            const refreshToken = response.refreshToken

            localStorage.setItem('access_token', accessToken);
            localStorage.setItem('refresh_token', refreshToken);
            setToken(accessToken);
            setRefreshToken(refreshToken);

            // 2. Wyciągamy ID użytkownika z tokenu
            const userId = getUserIdFromToken(accessToken);

            if (!userId) {
                throw new Error("Nie udało się pobrać ID użytkownika z tokenu");
            }

            // 3. Sprawdzamy, czy profil lekarza istnieje, używając jego ID
            try {
                // Pobieramy dane z MedicalStaffController -> @GetMapping("/{id}")
                await axios.get(`http://localhost:8082/api/v1/staff/${userId}`, {
                    headers: { Authorization: `Bearer ${accessToken}` }
                });

                // Jeśli profil istnieje (status 200 OK), idziemy do listy pacjentów
                // navigate('/patients');
            } catch (profileErr: any) {
                // Jeśli dostaniemy status 404 (Not Found), kierujemy na konfigurację profilu
                if (profileErr.response && (profileErr.response.status === 404 || profileErr.response.status === 400)) {
                    navigate('/setup');
                } else {
                    console.error('Błąd pobierania profilu:', profileErr);
                    // W razie innego błędu (np. 500) awaryjnie wpuszczamy do aplikacji
                    navigate('/patients');
                }
            }

        } catch (err) {
            if (axios.isAxiosError(err)) {
                if (err.status === 401) {
                    setError("Nieprawidłowy email lub hasło.");
                } else {
                    setError("Błąd podczas logowania. Spróbuj ponownie później.");
                }
            } else {
                setError('Brak połączenia z serwerem.');
            }
            setError('');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h2 className="login-title">Witaj ponownie</h2>
                <p className="login-subtitle">Zaloguj się do panelu medycznego HealthMonitor</p>

                {error && <div className="error-message">{error}</div>}

                <form className="login-form" onSubmit={handleLogin}>
                    <div className="input-group">
                        <label className="input-label">Email</label>
                        <input
                            type="text"
                            className="login-input"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="Wpisz nazwę użytkownika"
                            required
                            disabled={isLoading}
                        />
                    </div>

                    <div className="input-group">
                        <label className="input-label">Hasło</label>
                        <input
                            type="password"
                            className="login-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
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
                            cursor: isLoading ? 'wait' : 'pointer'
                        }}
                    >
                        {isLoading ? 'Logowanie...' : 'Zaloguj się'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default Login;