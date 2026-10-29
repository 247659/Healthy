import React, {useState, useEffect, useMemo} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer} from 'recharts';
import {
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Typography,
    Pagination,
    Box,
    Chip,
    Stack,
    Divider,
    CircularProgress
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ErrorIcon from '@mui/icons-material/Error';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import InfoIcon from '@mui/icons-material/Info';

import type {Patient} from "../../types/patient.ts";
import {vitalService} from "../../api/vitalClient.ts";
import {notificationService} from "../../api/notificationClient.ts";
import {useAuthStore} from "../../store/auth-store.ts";
import type {Notification} from "../../types/notification.ts";

interface VitalSigns {
    patientId?: string;
    timestamp: string;
    measurements: {
        heartRate?: number;
        spO2?: number;
        temperature?: number;
        bloodPressure?: {
            systolic?: number;
            diastolic?: number;
        };
    };
}

const ALERTS_PAGE_SIZE = 8;

const PatientDetails = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const user = useAuthStore(state => state.user);

    const patient = location.state?.patient as Patient | undefined;
    const highlightAlertId = location.state?.highlightAlertId as string | undefined;
    const token = localStorage.getItem('access_token');

    const [historyVitals, setHistoryVitals] = useState<VitalSigns[]>([]);
    const [latestVitals, setLatestVitals] = useState<VitalSigns | null>(null);
    const [isVitalsLoading, setIsVitalsLoading] = useState(true);

    // --- STANY DLA ALERTÓW I PAGINACJI SERWEROWEJ ---
    const [alerts, setAlerts] = useState<Notification[]>([]);
    const [isAlertsLoading, setIsAlertsLoading] = useState<boolean>(true);
    const [alertPage, setAlertPage] = useState<number>(1); // MUI Pagination zaczyna od 1
    const [totalAlertPages, setTotalAlertPages] = useState<number>(0);
    const [totalAlertsCount, setTotalAlertsCount] = useState<number>(0);
    const [expandedAlertId, setExpandedAlertId] = useState<string | false>(highlightAlertId || false);

    // --- DANE WYKRESÓW ---
    const chartData = useMemo(() => {
        return [...historyVitals].reverse().map(v => ({
            time: new Date(v.timestamp).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'}),
            heartRate: v.measurements?.heartRate,
            systolic: v.measurements?.bloodPressure?.systolic,
            diastolic: v.measurements?.bloodPressure?.diastolic,
            spO2: v.measurements?.spO2,
            temperature: v.measurements?.temperature
        }));
    }, [historyVitals]);

    // 1. USE_EFFECT DLA PARAMETRÓW ŻYCIOWYCH (HISTORIA + LIVE)
    useEffect(() => {
        if (!patient || !token || !user) {
            navigate('/patients');
            return;
        }

        const fetchHistory = async () => {
            setIsVitalsLoading(true);
            try {
                const historyRes = await vitalService.getHistory(patient.id);
                const data = Array.isArray(historyRes) ? historyRes : [];
                setHistoryVitals(data);
                if (data.length > 0) {
                    setLatestVitals(data[0]);
                }
            } catch (err) {
                console.error("Błąd pobierania historii pomiarów:", err);
            } finally {
                setIsVitalsLoading(false);
            }
        };

        const fetchLiveVitals = async () => {
            try {
                const oneMinuteAgo = new Date(Date.now() - 60000).toISOString();
                const res = await vitalService.getHistoryByTime(patient.id, oneMinuteAgo);
                const data = Array.isArray(res) ? res : [];
                if (data.length > 0) {
                    setLatestVitals(data[0]);
                }
            } catch {
                // ignorowane w tle
            }
        };

        fetchHistory();
        const intervalId = setInterval(fetchLiveVitals, 5000);

        return () => {
            clearInterval(intervalId);
        };
    }, [patient, token, user, navigate]);

    // 2. OSOBNY USE_EFFECT DLA PAGINOWANYCH ALERTÓW Z BACKENDU
    useEffect(() => {
        if (!patient || !token || !user) return;

        const fetchPaginatedAlerts = async () => {
            setIsAlertsLoading(true);
            try {
                // Przekazujemy alertPage - 1, ponieważ Spring Boot numeruje strony od 0
                const pageRes = await notificationService.getAllNotifications(
                    patient.id,
                    user.id,
                    alertPage - 1,
                    ALERTS_PAGE_SIZE
                );

                if (pageRes && Array.isArray(pageRes.content)) {
                    setAlerts(pageRes.content);
                    setTotalAlertPages(pageRes.totalPages ?? 0);
                    setTotalAlertsCount(pageRes.totalElements ?? 0);
                } else if (Array.isArray(pageRes)) {
                    // Fallback, gdyby endpoint zwrócił zwykłą tablicę
                    setAlerts(pageRes);
                    setTotalAlertPages(1);
                    setTotalAlertsCount(pageRes.length);
                }
            } catch (err) {
                console.error("Błąd pobierania alertów:", err);
                setAlerts([]);
            } finally {
                setIsAlertsLoading(false);
            }
        };

        fetchPaginatedAlerts();
    }, [patient, token, user, alertPage]);

    useEffect(() => {
        if (highlightAlertId) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setExpandedAlertId(highlightAlertId);
            setAlertPage(1); // Najnowsze powiadomienia z dzwonka znajdują się na 1. stronie
        }
    }, [highlightAlertId]);

    useEffect(() => {
        if (!isAlertsLoading && highlightAlertId && alerts.some(a => a.alertId === highlightAlertId)) {
            const element = document.getElementById(`alert-item-${highlightAlertId}`);
            if (element) {
                element.scrollIntoView({behavior: 'smooth', block: 'center'});
            }
        }
    }, [isAlertsLoading, alerts, highlightAlertId]);

    // --- HANDLERY PAGINACJI I AKORDEONU ---
    const handlePageChange = (_event: React.ChangeEvent<unknown>, value: number) => {
        setAlertPage(value);
        setExpandedAlertId(false); // Zwiń otwarte szczegóły po przejściu na inną stronę
    };

    const handleAccordionChange = (alertId: string) => (_event: React.SyntheticEvent, isExpanded: boolean) => {
        setExpandedAlertId(isExpanded ? alertId : false);
    };

    const getSeverityConfig = (alert: Notification) => {
        const sev = alert.severity?.toUpperCase() || '';
        if (sev === 'CRITICAL' || sev === 'HIGH' || alert.riskScore > 0.8) {
            return {
                color: '#dc2626',
                bgColor: '#fff5f5',
                borderColor: '#feb2b2',
                chipColor: 'error' as const,
                icon: <ErrorIcon sx={{color: '#dc2626'}}/>
            };
        }
        if (sev === 'WARNING' || sev === 'MEDIUM' || alert.riskScore > 0.5) {
            return {
                color: '#d97706',
                bgColor: '#fffbeb',
                borderColor: '#fde68a',
                chipColor: 'warning' as const,
                icon: <WarningAmberIcon sx={{color: '#d97706'}}/>
            };
        }
        return {
            color: '#2563eb',
            bgColor: '#eff6ff',
            borderColor: '#bfdbfe',
            chipColor: 'info' as const,
            icon: <InfoIcon sx={{color: '#2563eb'}}/>
        };
    };

    // --- STATYSTYKI ---
    const getStats = (selector: (v: VitalSigns) => number | undefined) => {
        const values = historyVitals.map(selector).filter(v => v !== undefined && v !== null) as number[];
        if (values.length === 0) return {min: '--', max: '--'};
        return {min: Math.min(...values), max: Math.max(...values)};
    };

    const hrStats = getStats(v => v.measurements?.heartRate);
    const sysStats = getStats(v => v.measurements?.bloodPressure?.systolic);
    const diaStats = getStats(v => v.measurements?.bloodPressure?.diastolic);
    const spo2Stats = getStats(v => v.measurements?.spO2);
    const tempStats = getStats(v => v.measurements?.temperature);

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f4f7f6', padding: '20px 5%', fontFamily: "'Inter', system-ui, sans-serif" }}>
            <div style={{ maxWidth: '1400px', margin: '0 auto', width: '100%' }}>

                {/* Nagłówek powrotu */}
                <div style={{display: 'flex', alignItems: 'center', marginBottom: '30px', gap: '20px'}}>
                    <button
                        onClick={() => navigate('/patients')}
                        style={{
                            background: '#fff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '10px 15px',
                            color: '#4a5568', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px',
                            fontSize: '14px', fontWeight: '600', boxShadow: '0 2px 4px rgba(0,0,0,0.02)'
                        }}
                    >
                        <span>&larr;</span> Wróć
                    </button>
                    <h2 style={{color: '#2d3748', margin: 0, fontSize: '24px', fontWeight: '700'}}>
                        {patient?.firstName} {patient?.lastName}
                    </h2>
                </div>

                {/* Info i statystyki */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px', marginBottom: '20px' }}>
                    <div style={{ backgroundColor: '#fff', padding: '24px', borderRadius: '20px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
                        <h3 style={{ marginTop: 0, color: '#4a5568', fontSize: '16px', marginBottom: '20px', borderBottom: '1px solid #edf2f7', paddingBottom: '10px' }}>Informacje</h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', fontSize: '15px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>PESEL</span> <strong style={{ color: '#2d3748' }}>{patient?.pesel || 'Brak'}</strong></div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>Wiek</span> <strong style={{ color: '#2d3748' }}>{patient?.dateOfBirth || 'Brak'}</strong></div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>Telefon</span> <strong style={{ color: '#2d3748' }}>{patient?.phoneNumber || 'Brak'}</strong></div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ color: '#a0aec0' }}>Adres</span> <strong style={{ color: '#2d3748', textAlign: 'right' }}>{patient?.address || 'Brak'}</strong></div>
                        </div>
                    </div>

                    <div style={{ backgroundColor: '#fff', padding: '24px', borderRadius: '20px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
                        <h3 style={{ marginTop: 0, color: '#4a5568', fontSize: '16px', marginBottom: '20px', borderBottom: '1px solid #edf2f7', paddingBottom: '10px' }}>Statystyki z 24h (Min / Max)</h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Tętno (bpm)</span>
                                <strong style={{ color: '#d9534f', fontSize: '16px' }}>{hrStats.min} - {hrStats.max}</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Ciśnienie (mmHg)</span>
                                <strong style={{ color: '#0275d8', fontSize: '16px' }}>{sysStats.min}/{diaStats.min} - {sysStats.max}/{diaStats.max}</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Saturacja (%)</span>
                                <strong style={{ color: '#5cb85c', fontSize: '16px' }}>{spo2Stats.min} - {spo2Stats.max}</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: '#a0aec0', fontSize: '14px' }}>Temperatura (°C)</span>
                                <strong style={{ color: '#ed8936', fontSize: '16px' }}>{tempStats.min} - {tempStats.max}</strong>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Parametry live */}
                <h3 style={{ marginTop: '30px', color: '#2d3748', fontSize: '18px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                    Bieżące parametry życiowe
                    <span style={{ fontSize: '11px', backgroundColor: '#fed7d7', color: '#c53030', padding: '3px 8px', borderRadius: '12px', fontWeight: 'bold' }}>LIVE (odświeżane co 5s)</span>
                </h3>

                {isVitalsLoading ? (
                    <p style={{ color: '#718096' }}>Pobieranie historii z bazy danych...</p>
                ) : latestVitals ? (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px', marginBottom: '40px' }}>
                        <div style={{ backgroundColor: '#fff5f5', padding: '24px', borderRadius: '20px', border: '1px solid #fed7d7', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#c53030', fontWeight: '600', marginBottom: '5px' }}>TĘTNO</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#e53e3e' }}>
                                {latestVitals.measurements?.heartRate ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>bpm</span>
                            </div>
                        </div>

                        <div style={{ backgroundColor: '#ebf8ff', padding: '24px', borderRadius: '20px', border: '1px solid #bee3f8', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#2b6cb0', fontWeight: '600', marginBottom: '5px' }}>CIŚNIENIE KRWI</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#3182ce' }}>
                                {latestVitals.measurements?.bloodPressure?.systolic ?? '--'}<span style={{ fontSize: '28px', color: '#63b3ed' }}>/</span>{latestVitals.measurements?.bloodPressure?.diastolic ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>mmHg</span>
                            </div>
                        </div>

                        <div style={{ backgroundColor: '#f0fff4', padding: '24px', borderRadius: '20px', border: '1px solid #c6f6d5', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#2f855a', fontWeight: '600', marginBottom: '5px' }}>SATURACJA SPO2</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#38a169' }}>
                                {latestVitals.measurements?.spO2 ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>%</span>
                            </div>
                        </div>

                        <div style={{ backgroundColor: '#fffff0', padding: '24px', borderRadius: '20px', border: '1px solid #feebc8', transition: 'all 0.3s ease' }}>
                            <div style={{ fontSize: '14px', color: '#c05621', fontWeight: '600', marginBottom: '5px' }}>TEMPERATURA</div>
                            <div style={{ fontSize: '42px', fontWeight: '800', color: '#dd6b20' }}>
                                {latestVitals.measurements?.temperature ?? '--'} <span style={{ fontSize: '16px', fontWeight: '500' }}>°C</span>
                            </div>
                        </div>
                    </div>
                ) : (
                    <p style={{color: '#718096'}}>Brak zapisanych pomiarów dla tego pacjenta.</p>
                )}

                {/* --- SEKCJA ALERTÓW Z PAGINACJĄ SERWEROWĄ --- */}
                <Box sx={{
                    backgroundColor: '#fff',
                    p: 3,
                    borderRadius: '20px',
                    mb: 3,
                    boxShadow: '0 4px 12px rgba(0,0,0,0.03)'
                }}>
                    <Stack
                        direction="row"
                        sx={{justifyContent: 'space-between', alignItems: 'center', mb: 2}}
                    >
                        <Typography variant="h6" sx={{
                            color: '#e53e3e',
                            fontWeight: 700,
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1
                        }}>
                            🚨 Historia alertów
                        </Typography>
                    </Stack>

                    {isAlertsLoading ? (
                        <Box sx={{display: 'flex', justifyContent: 'center', py: 4}}>
                            <CircularProgress size={32} sx={{color: '#e53e3e'}}/>
                        </Box>
                    ) : alerts.length > 0 ? (
                        <>
                            <Stack spacing={1.5}>
                                {alerts.map((alert, index) => {
                                    const uniqueKey = alert.alertId || `alert-${alertPage}-${index}`;
                                    const config = getSeverityConfig(alert);

                                    return (
                                        <Accordion
                                            id={`alert-item-${uniqueKey}`}
                                            key={uniqueKey}
                                            expanded={expandedAlertId === uniqueKey}
                                            onChange={handleAccordionChange(uniqueKey)}
                                            disableGutters
                                            elevation={0}
                                            sx={{
                                                backgroundColor: config.bgColor,
                                                border: `1px solid ${config.borderColor}`,
                                                borderLeft: `5px solid ${config.color}`,
                                                borderRadius: '10px !important',
                                                '&:before': {display: 'none'},
                                                overflow: 'hidden',
                                            }}
                                        >
                                            <AccordionSummary
                                                expandIcon={<ExpandMoreIcon sx={{color: config.color}}/>}
                                                sx={{px: 2, py: 0.5}}
                                            >
                                                <Stack
                                                    direction={{xs: 'column', sm: 'row'}}
                                                    spacing={1.5}
                                                    sx={{
                                                        alignItems: {xs: 'flex-start', sm: 'center'},
                                                        justifyContent: 'space-between',
                                                        width: '100%',
                                                        pr: 1
                                                    }}
                                                >
                                                    <Stack direction="row" spacing={1.5} sx={{alignItems: 'center'}}>
                                                        {config.icon}
                                                        <Box>
                                                            <Typography variant="subtitle2"
                                                                        sx={{fontWeight: 700, color: '#1e293b'}}>
                                                                {alert.message || 'Wykryto nieprawidłowość.'}
                                                            </Typography>
                                                            <Typography variant="caption" sx={{color: '#64748b'}}>
                                                                {alert.timestamp ? new Date(alert.timestamp).toLocaleString() : 'Brak daty'}
                                                            </Typography>
                                                        </Box>
                                                    </Stack>

                                                    <Stack direction="row" spacing={1} sx={{alignItems: 'center'}}>
                                                        {alert.severity && (
                                                            <Chip
                                                                label={alert.severity}
                                                                color={config.chipColor}
                                                                size="small"
                                                                sx={{fontWeight: 600, fontSize: '0.7rem', height: 22}}
                                                            />
                                                        )}
                                                        <Chip
                                                            label={`Ryzyko: ${(alert.riskScore ?? 0).toFixed(2)}`}
                                                            variant="outlined"
                                                            size="small"
                                                            sx={{
                                                                fontWeight: 600,
                                                                fontSize: '0.7rem',
                                                                height: 22,
                                                                borderColor: config.color,
                                                                color: config.color
                                                            }}
                                                        />
                                                    </Stack>
                                                </Stack>
                                            </AccordionSummary>

                                            <AccordionDetails sx={{
                                                backgroundColor: 'rgba(255, 255, 255, 0.75)',
                                                pt: 1.5,
                                                pb: 2,
                                                px: 2.5
                                            }}>
                                                <Divider sx={{mb: 1.5, borderColor: config.borderColor}}/>
                                                <Typography variant="caption" sx={{
                                                    fontWeight: 700,
                                                    color: '#475569',
                                                    textTransform: 'uppercase',
                                                    letterSpacing: 0.5
                                                }}>
                                                    Szczegóły alertu:
                                                </Typography>

                                                {alert.details && alert.details.length > 0 ? (
                                                    <Box component="ul" sx={{
                                                        m: 0,
                                                        mt: 0.5,
                                                        pl: 2.5,
                                                        color: '#334155',
                                                        fontSize: '0.875rem'
                                                    }}>
                                                        {alert.details.map((detail, idx) => (
                                                            <li key={idx} style={{marginBottom: '4px'}}>
                                                                <Typography variant="body2" sx={{color: '#334155'}}>
                                                                    {detail}
                                                                </Typography>
                                                            </li>
                                                        ))}
                                                    </Box>
                                                ) : (
                                                    <Typography variant="body2"
                                                                sx={{color: '#64748b', mt: 0.5, fontStyle: 'italic'}}>
                                                        Brak dodatkowych szczegółów dla tego zdarzenia.
                                                    </Typography>
                                                )}

                                                <Stack
                                                    direction="row"
                                                    sx={{
                                                        justifyContent: 'space-between',
                                                        mt: 1.5,
                                                        pt: 1,
                                                        borderTop: '1px dashed #e2e8f0'
                                                    }}
                                                >
                                                    <Typography variant="caption" color="text.secondary">
                                                        ID Alertu: {alert.alertId || 'Brak'}
                                                    </Typography>
                                                </Stack>
                                            </AccordionDetails>
                                        </Accordion>
                                    );
                                })}
                            </Stack>

                            {/* Kontrolka paginacji sterowana odpowiedzią z Page<AlertDto> */}
                            {totalAlertPages > 1 && (
                                <Box sx={{display: 'flex', justifyContent: 'right', mt: 3}}>
                                    <Pagination
                                        count={totalAlertPages}
                                        page={alertPage}
                                        onChange={handlePageChange}
                                        color="primary"
                                        shape="rounded"
                                    />
                                    {totalAlertsCount > 0 && (
                                        <Chip
                                            label={`Łącznie: ${totalAlertsCount}`}
                                            size="medium"
                                            sx={{fontWeight: 600, backgroundColor: '#edf2f7', color: '#4a5568'}}
                                        />
                                    )}
                                </Box>
                            )}
                        </>
                    ) : (
                        <Typography variant="body2" sx={{color: '#718096'}}>
                            Brak alertów dla pacjenta.
                        </Typography>
                    )}
                </Box>

                {/* WYKRESY */}
                <div style={{ backgroundColor: '#fff', padding: '30px', borderRadius: '24px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
                    <h3 style={{ marginTop: 0, color: '#2d3748', fontSize: '18px', marginBottom: '30px' }}>Historia wykresów (statyczna)</h3>
                    {historyVitals.length > 0 ? (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '50px' }}>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#e53e3e', fontWeight: '600' }}>Wykres Tętna</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={['dataMin - 10', 'dataMax + 10']} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="heartRate" stroke="#e53e3e" strokeWidth={3} dot={{r: 4, fill: '#e53e3e'}} activeDot={{r: 8}} name="Tętno" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#3182ce', fontWeight: '600' }}>Wykres Ciśnienia</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={['auto', 'auto']} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="systolic" stroke="#3182ce" strokeWidth={3} dot={false} name="SYS (Skurczowe)" />
                                        <Line type="monotone" dataKey="diastolic" stroke="#63b3ed" strokeWidth={3} dot={false} name="DIA (Rozkurczowe)" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#38a169', fontWeight: '600' }}>Wykres Saturacji SpO2</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={[80, 100]} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="spO2" stroke="#38a169" strokeWidth={3} dot={{r: 4, fill: '#38a169'}} activeDot={{r: 8}} name="SpO2" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                            <div style={{ width: '100%', height: '400px' }}>
                                <h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#dd6b20', fontWeight: '600' }}>Wykres Temperatury</h4>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ left: -10, right: 20 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#edf2f7" />
                                        <XAxis dataKey="time" tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <YAxis domain={[35, 42]} tick={{fontSize: 12, fill: '#a0aec0'}} axisLine={false} tickLine={false} />
                                        <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                        <Line type="monotone" dataKey="temperature" stroke="#dd6b20" strokeWidth={3} dot={{r: 4, fill: '#dd6b20'}} activeDot={{r: 8}} name="Temperatura" />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>

                        </div>
                    ) : (
                        <p style={{color: '#a0aec0'}}>Brak wystarczającej ilości danych do wygenerowania wykresów.</p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PatientDetails;