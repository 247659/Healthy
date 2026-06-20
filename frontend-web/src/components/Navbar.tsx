import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from "../api/authClient.ts";
import { useNotifications } from '../hooks/useNotifications';

// --- Importy MUI ---
import {
    AppBar,
    Toolbar,
    Typography,
    Button,
    IconButton,
    Badge,
    Menu,
    MenuItem,
    Box,
    ListItemIcon,
    ListItemText,
    Divider
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import ErrorIcon from '@mui/icons-material/Error';

interface NavbarProps {
    token: string | null;
    setToken: (token: string | null) => void;
    refreshToken: string | null;
    setRefreshToken: (refreshToken: string | null) => void;
}

const Navbar = ({ token, setToken, refreshToken, setRefreshToken }: NavbarProps) => {
    const navigate = useNavigate();
    const { alerts, unreadCount, markAllAsRead } = useNotifications();

    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const openNotifications = Boolean(anchorEl);

    const handleNotificationClick = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
        if (unreadCount > 0) {
            markAllAsRead();
        }
    };

    const handleNotificationClose = () => {
        setAnchorEl(null);
    };

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
                            {/* --- DZWONEK POWIADOMIEŃ --- */}
                            <IconButton
                                onClick={handleNotificationClick}
                                aria-controls={openNotifications ? 'notifications-menu' : undefined}
                                aria-haspopup="true"
                                aria-expanded={openNotifications ? 'true' : undefined}
                                sx={{ color: '#64748b' }} // Szarawy kolor pasujący do jasnego motywu
                            >
                                <Badge badgeContent={unreadCount} color="error">
                                    <NotificationsIcon />
                                </Badge>
                            </IconButton>

                            {/* --- ROZWIJANA LISTA POWIADOMIEŃ --- */}
                            <Menu
                                id="notifications-menu"
                                anchorEl={anchorEl}
                                open={openNotifications}
                                onClose={handleNotificationClose}
                                PaperProps={{
                                    elevation: 4,
                                    sx: { width: 350, maxHeight: 400, mt: 1.5 }
                                }}
                                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                            >
                                <Box sx={{ px: 2, py: 1 }}>
                                    <Typography variant="subtitle1" fontWeight="bold">Powiadomienia</Typography>
                                </Box>
                                <Divider />

                                {alerts.length === 0 ? (
                                    <MenuItem disabled sx={{ py: 3, justifyContent: 'center' }}>
                                        <Typography variant="body2" color="text.secondary">
                                            Brak nowych powiadomień
                                        </Typography>
                                    </MenuItem>
                                ) : (
                                    alerts.map((alert, index) => {
                                        const isCritical = alert.riskScore > 0.8;
                                        return (
                                            <MenuItem
                                                key={index}
                                                onClick={handleNotificationClose}
                                                sx={{
                                                    borderLeft: `4px solid ${isCritical ? '#dc2626' : '#f59e0b'}`,
                                                    alignItems: 'flex-start',
                                                    py: 1.5
                                                }}
                                            >
                                                <ListItemIcon sx={{ mt: 0.5 }}>
                                                    {isCritical ? <ErrorIcon sx={{ color: '#dc2626' }} /> : <WarningAmberIcon sx={{ color: '#f59e0b' }} />}
                                                </ListItemIcon>
                                                <ListItemText
                                                    primary={`Pacjent ID: ${alert.patientId}`}
                                                    primaryTypographyProps={{ variant: 'subtitle2', fontWeight: 'bold' }}
                                                    secondary={
                                                        <React.Fragment>
                                                            <Typography variant="body2" color="text.primary" sx={{ display: 'block' }}>
                                                                {alert.message}
                                                            </Typography>
                                                            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                                                                Ryzyko: {alert.riskScore.toFixed(2)} • {new Date(alert.timestamp).toLocaleTimeString()}
                                                            </Typography>
                                                        </React.Fragment>
                                                    }
                                                />
                                            </MenuItem>
                                        );
                                    })
                                )}
                            </Menu>

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