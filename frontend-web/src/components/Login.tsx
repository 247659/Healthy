// frontend/web/src/components/Login.tsx
import { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import './Login.css';

const Login = ({ setToken }: { setToken: (token: string) => void }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    // 1. Dodajemy nowy stan sprawdzający, czy trwa wysyłanie zapytania
    const [isLoading, setIsLoading] = useState(false);

    const navigate = useNavigate();

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        // 2. Włączamy stan ładowania tuż przed startem komunikacji
        setIsLoading(true);

        try {
            const response = await axios.post('http://localhost:8087/api/v1/auth/login', {
                email: email,
                password: password
            });

            const accessToken = response.data.accessToken || response.data.token;

            localStorage.setItem('access_token', accessToken);
            setToken(accessToken);

            navigate('/patients');
        } catch (err) {
            console.error('Błąd logowania:', err);
            setError('Nieprawidłowy login lub hasło.');
        } finally {
            // 3. Blok finally WYKONA SIĘ ZAWSZE - niezależnie czy logowanie się uda, czy wyrzuci błąd.
            // Dzięki temu przycisk nie "zawiśnie" w stanie ładowania w przypadku błędu.
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
                        <label className="input-label">Login</label>
                        <input
                            type="text"
                            className="login-input"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="Wpisz nazwę użytkownika"
                            required
                            disabled={isLoading} /* Blokujemy pole podczas ładowania */
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
                            disabled={isLoading} /* Blokujemy pole podczas ładowania */
                        />
                    </div>

                    {/* 4. Modyfikujemy przycisk na podstawie stanu isLoading */}
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

                <div className="register-link-container">
                    Nie masz jeszcze konta?
                    {/* Jeśli ładuje, chowamy link lub go wyłączamy, by użytkownik nie uciekł podczas logowania */}
                    {isLoading ? (
                        <span className="register-link" style={{ cursor: 'wait', color: '#9ca3af' }}> Zarejestruj się</span>
                    ) : (
                        <Link to="/register" className="register-link">Zarejestruj się</Link>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Login;