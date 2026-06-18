// frontend/web/src/components/Navbar.tsx
import { useNavigate } from 'react-router-dom';
import './Navbar.css';
import {authService} from "../api/authClient.ts";

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

    return (
        <nav className="navbar">
            <h2 className="navbar-brand">
                <span style={{ fontSize: '24px' }}>🏥</span> HealthMonitor
            </h2>
            <div>
                {token ? (
                    <button onClick={handleLogout} className="logout-btn">
                        Wyloguj się
                    </button>
                ) : (
                    <span className="navbar-user-info">
                        Zaloguj się, aby uzyskać dostęp
                    </span>
                )}
            </div>
        </nav>
    );
};

export default Navbar;