import { useNavigate } from 'react-router-dom';
import './Navbar.css';
import { authService } from "../api/authClient.ts";

interface NavbarProps {
    token: string | null;
    setToken: (token: string | null) => void;
    refreshToken: string | null;
    setRefreshToken: (refreshToken: string | null) => void;
}

const Navbar = ({ token, setToken, refreshToken, setRefreshToken }: NavbarProps) => {
    const navigate = useNavigate();

    const handleLogout = () => {
        authService.logout(refreshToken);
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        setToken(null);
        setRefreshToken(null);
        navigate('/');
    };

    const goToProfile = () => {
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                navigate(`/profile/${payload.sub}`);
            } catch (error) {
                console.error("Błąd nawigacji do profilu", error);
            }
        }
    };

    return (
        <nav className="navbar">
            <div className="navbar-container">
                <h2 className="navbar-brand" onClick={() => navigate('/patients')}>
                    <span className="brand-icon">🏥</span>
                    <span className="brand-text">HealthMonitor</span>
                </h2>

                <div className="nav-links">
                    {token ? (
                        <>
                            <button onClick={() => navigate('/patients')} className="nav-btn nav-btn-secondary">
                                Pacjenci
                            </button>
                            <button onClick={goToProfile} className="nav-btn nav-btn-primary">
                                Profil
                            </button>
                            <button onClick={handleLogout} className="nav-btn nav-btn-logout">
                                Wyloguj
                            </button>
                        </>
                    ) : (
                        <span className="nav-guest-text">
                            Zaloguj się, aby uzyskać dostęp
                        </span>
                    )}
                </div>
            </div>
        </nav>
    );
};

export default Navbar;