// frontend/web/src/components/Navbar.tsx
import { useNavigate } from 'react-router-dom';
import './Navbar.css';

interface NavbarProps {
    token: string | null;
    setToken: (token: string | null) => void;
}

const Navbar = ({ token, setToken }: NavbarProps) => {
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem('access_token');
        setToken(null);
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