import {
    Badge,
    Box,
    Button,
    Checkbox,
    Divider,
    FormControlLabel,
    IconButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Typography
} from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import ErrorIcon from "@mui/icons-material/Error";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import React, {useState} from "react";
import {useNavigate} from "react-router-dom";
import {useNotifications} from "../hooks/useNotifications.ts";
import {usePatientStore} from "../store/patients-store.ts";

const NotificationBell = () => {
    const {alerts, unreadCount, markAsRead} = useNotifications();
    const navigate = useNavigate();

    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const [selectedIndices, setSelectedIndices] = useState<number[]>([]);

    const openNotifications = Boolean(anchorEl);
    const assignedPatients = usePatientStore(state => state.assignedPatients);

    const handleNotificationClick = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleNotificationClose = () => {
        setAnchorEl(null);
        setSelectedIndices([]);
    };

    const handleToggleSelect = (index: number, event?: React.MouseEvent) => {
        if (event) {
            event.stopPropagation();
        }
        setSelectedIndices(prev =>
            prev.includes(index)
                ? prev.filter(i => i !== index)
                : [...prev, index]
        );
    };

    const handleSelectAll = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.checked) {
            setSelectedIndices(alerts.map((_, index) => index));
        } else {
            setSelectedIndices([]);
        }
    };

    const handleMarkSelectedAsRead = () => {
        const selectedAlerts = selectedIndices.map(index => alerts[index].alertId);
        markAsRead(selectedAlerts);
        setSelectedIndices([]);
    };

    // Przejście do PatientDetails z przekazaniem pacjenta i ID wybranego alertu
    const handleGoToPatientDetails = (event: React.MouseEvent, alertId: string, patientId: string) => {
        event.stopPropagation();
        const patient = assignedPatients.find((p) => p.id === patientId);
        if (!patient) return;

        handleNotificationClose();

        // Podmień '/patient-details' na swoją ścieżkę z routera, jeśli jest inna
        navigate(`/patient/${patientId}`, {
            state: {
                patient,
                highlightAlertId: alertId
            }
        });
    };

    const isAllSelected = alerts.length > 0 && selectedIndices.length === alerts.length;
    const isIndeterminate = selectedIndices.length > 0 && selectedIndices.length < alerts.length;

    return (
        <>
            <IconButton
                onClick={handleNotificationClick}
                aria-controls={openNotifications ? 'notifications-menu' : undefined}
                aria-haspopup="true"
                aria-expanded={openNotifications ? 'true' : undefined}
                sx={{color: '#64748b'}}
            >
                <Badge badgeContent={unreadCount} color="error">
                    <NotificationsIcon/>
                </Badge>
            </IconButton>

            {/* --- ROZWIJANA LISTA POWIADOMIEŃ --- */}
            <Menu
                id="notifications-menu"
                anchorEl={anchorEl}
                open={openNotifications}
                onClose={handleNotificationClose}
                slotProps={{
                    paper: {
                        elevation: 4,
                        sx: {width: 440, maxHeight: 450, mt: 1.5}
                    }
                }}
                transformOrigin={{horizontal: 'right', vertical: 'top'}}
                anchorOrigin={{horizontal: 'right', vertical: 'bottom'}}
            >
                {/* --- NAGŁÓWEK MENU --- */}
                <Box sx={{
                    px: 2,
                    pt: 1,
                    pb: 0.5,
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                }}>
                    <Typography variant="subtitle1" sx={{fontWeight: 'bold'}}>
                        Powiadomienia
                    </Typography>

                    {alerts.length > 0 && (
                        <Button
                            size="small"
                            startIcon={<DoneAllIcon/>}
                            disabled={selectedIndices.length === 0}
                            onClick={handleMarkSelectedAsRead}
                            sx={{textTransform: 'none', fontSize: '0.8rem'}}
                        >
                            Oznacz jako przeczytane ({selectedIndices.length})
                        </Button>
                    )}
                </Box>

                {/* --- PASEK "ZAZNACZ WSZYSTKIE" --- */}
                {alerts.length > 0 && (
                    <Box sx={{px: 2, pb: 0.5, display: 'flex', alignItems: 'center'}}>
                        <FormControlLabel
                            control={
                                <Checkbox
                                    size="small"
                                    checked={isAllSelected}
                                    indeterminate={isIndeterminate}
                                    onChange={handleSelectAll}
                                />
                            }
                            label={
                                <Typography variant="caption" color="text.secondary">
                                    Zaznacz wszystkie
                                </Typography>
                            }
                        />
                    </Box>
                )}
                <Divider/>

                {/* --- LISTA POWIADOMIEŃ --- */}
                {alerts.length === 0 ? (
                    <MenuItem disabled sx={{py: 3, justifyContent: 'center'}}>
                        <Typography variant="body2" color="text.secondary">
                            Brak nowych powiadomień
                        </Typography>
                    </MenuItem>
                ) : (
                    alerts.map((alert, index) => {
                        const patient = assignedPatients.find((p) => p.id === alert.patientId);
                        const isCritical = alert.riskScore > 0.8;
                        const isSelected = selectedIndices.includes(index);

                        return (
                            <MenuItem
                                key={alert.alertId || index}
                                onClick={() => handleToggleSelect(index)}
                                selected={isSelected}
                                sx={{
                                    borderLeft: `4px solid ${isCritical ? '#dc2626' : '#f59e0b'}`,
                                    alignItems: 'flex-start',
                                    py: 1.5,
                                    px: 1.5
                                }}
                            >
                                {/* Checkbox z lewej strony */}
                                <Checkbox
                                    size="small"
                                    checked={isSelected}
                                    onClick={(e) => handleToggleSelect(index, e)}
                                    sx={{p: 0.5, mr: 1, mt: 0.2}}
                                />

                                <ListItemIcon sx={{mt: 0.5, minWidth: 32}}>
                                    {isCritical ? (
                                        <ErrorIcon sx={{color: '#dc2626'}} fontSize="small"/>
                                    ) : (
                                        <WarningAmberIcon sx={{color: '#f59e0b'}} fontSize="small"/>
                                    )}
                                </ListItemIcon>

                                <ListItemText
                                    primary={
                                        <Typography variant="subtitle2" sx={{fontWeight: 'bold'}}>
                                            Pacjent: {patient ? `${patient.firstName} ${patient.lastName}` : `ID: ${alert.patientId}`}
                                        </Typography>
                                    }
                                    secondary={
                                        <React.Fragment>
                                            <Typography
                                                variant="body2"
                                                color="text.primary"
                                                sx={{
                                                    display: 'block',
                                                    whiteSpace: 'normal',
                                                    overflowWrap: 'anywhere',
                                                    wordBreak: 'break-word',
                                                }}
                                            >
                                                {alert.message}
                                            </Typography>
                                            <Typography
                                                variant="caption"
                                                color="text.secondary"
                                                sx={{display: 'block', mt: 0.5}}
                                            >
                                                Ryzyko: {alert.riskScore.toFixed(2)} • {new Date(alert.timestamp).toLocaleTimeString()}
                                            </Typography>

                                            {/* Przycisk przenoszący do PatientDetails */}
                                            {patient && (
                                                <Box sx={{mt: 1, display: 'flex', justifyContent: 'flex-end'}}>
                                                    <Button
                                                        size="small"
                                                        variant="outlined"
                                                        endIcon={<ArrowForwardIcon fontSize="small"/>}
                                                        onClick={(e) => handleGoToPatientDetails(e, alert.alertId, alert.patientId)}
                                                        sx={{
                                                            textTransform: 'none',
                                                            fontSize: '0.75rem',
                                                            py: 0.2,
                                                            px: 1
                                                        }}
                                                    >
                                                        Szczegóły alertu
                                                    </Button>
                                                </Box>
                                            )}
                                        </React.Fragment>
                                    }
                                />
                            </MenuItem>
                        );
                    })
                )}
            </Menu>
        </>
    );
};

export default NotificationBell;