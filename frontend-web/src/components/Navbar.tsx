import { useNavigate } from 'react-router-dom';
import { authService } from "../api/authClient.ts";

// --- Importy MUI ---
import {
    AppBar,
    Toolbar,
    Typography,
    Button,
    Box,
} from '@mui/material';
import NotificationBell from "./NotificationBell.tsx";
import {useAuthStore} from "../store/auth-store.ts";

interface NavbarProps {
    token: string | null;
    setToken: (token: string | null) => void;
    refreshToken: string | null;
    setRefreshToken: (refreshToken: string | null) => void;
}

const Navbar = ({ token, setToken, refreshToken, setRefreshToken }: NavbarProps) => {
    const navigate = useNavigate();
    const logout = useAuthStore(state => state.logout)

    const handleLogout = () => {
        authService.logout(refreshToken);
        logout()
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
        <AppBar
            position="sticky"
            sx={{
                bgcolor: '#ffffff', // Z CSS: background: #ffffff;
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)', // Z CSS: box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                zIndex: 1000 // Z CSS: z-index: 1000;
            }}
        >
            <Toolbar sx={{ justifyContent: 'space-between', padding: { xs: '0.5rem 1rem', md: '0.5rem 2rem' } }}>

                {/* --- LOGO (Odwzorowanie .navbar-brand) --- */}
                <Typography
                    variant="h6"
                    component="div"
                    onClick={() => navigate('/patients')}
                    sx={{
                        cursor: 'pointer',
                        fontWeight: 700, // Z CSS: font-weight: 700;
                        color: '#2563eb', // Z CSS: color: #2563eb;
                        fontSize: '1.5rem', // Z CSS: font-size: 1.5rem;
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem' // Z CSS: gap: 0.5rem;
                    }}
                >
                    <span>🏥</span> HealthMonitor
                </Typography>

                {/* --- SEKCJA PRZYCISKÓW (Odwzorowanie .nav-links) --- */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    {token ? (
                        <>
                            {/*/!* --- DZWONEK POWIADOMIEŃ --- *!/*/}
                            <NotificationBell />

                            {/* --- PRZYCISK PACJENCI (.nav-btn-secondary) --- */}
                            <Button
                                onClick={() => navigate('/patients')}
                                sx={{
                                    bgcolor: '#e2e8f0',
                                    color: '#1e293b',
                                    borderRadius: '0.5rem',
                                    fontWeight: 600,
                                    textTransform: 'none',
                                    padding: '0.5rem 1rem',
                                    '&:hover': { bgcolor: '#cbd5e1' }
                                }}
                            >
                                Pacjenci
                            </Button>

                            {/* --- PRZYCISK PROFIL (.nav-btn-primary) --- */}
                            <Button
                                onClick={goToProfile}
                                sx={{
                                    bgcolor: '#2563eb',
                                    color: 'white',
                                    borderRadius: '0.5rem',
                                    fontWeight: 600,
                                    textTransform: 'none',
                                    padding: '0.5rem 1rem',
                                    '&:hover': { bgcolor: '#1d4ed8' }
                                }}
                            >
                                Mój Profil
                            </Button>

                            {/* --- PRZYCISK WYLOGUJ (.nav-btn-logout) --- */}
                            <Button
                                onClick={handleLogout}
                                sx={{
                                    bgcolor: '#fee2e2',
                                    color: '#dc2626',
                                    borderRadius: '0.5rem',
                                    fontWeight: 600,
                                    textTransform: 'none',
                                    padding: '0.5rem 1rem',
                                    '&:hover': { bgcolor: '#fecaca' }
                                }}
                            >
                                Wyloguj
                            </Button>
                        </>
                    ) : (
                        <Typography variant="body2" sx={{ color: '#64748b' }}>
                            Zaloguj się, aby uzyskać dostęp
                        </Typography>
                    )}
                </Box>
            </Toolbar>
        </AppBar>
    );
};

export default Navbar;