import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator, TouchableOpacity, Dimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path, Polyline, Circle, Line, Defs, LinearGradient, Stop, Polygon } from 'react-native-svg';

const screenWidth = Dimensions.get("window").width;

const BackIcon = () => (
    <Svg width={20} height={20} viewBox="0 0 24 24" fill="none" stroke="#1F2937" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
        <Path d="M15 18l-6-6 6-6" />
    </Svg>
);

// --- NOWOCZESNY WYKRES: AREA CHART + SCROLL ---
const CustomLineChart = ({ data, color, unit }: { data: number[], color: string, unit: string }) => {
    const [activeIndex, setActiveIndex] = useState<number | null>(null);

    if (!data || data.length === 0) return <Text style={styles.noDataText}>Brak wystarczających danych</Text>;

    const CHART_HEIGHT = 160;
    const PADDING_TOP = 20;
    const PADDING_BOTTOM = 20;
    const innerHeight = CHART_HEIGHT - PADDING_TOP - PADDING_BOTTOM;

    // Skalowanie osi Y
    let min = Math.min(...data);
    let max = Math.max(...data);
    if (min === max) { min -= 1; max += 1; }
    const range = max - min;

    // DYNAMICZNA SZEROKOŚĆ: 35 pikseli na każdy punkt danych. Jeśli punktów jest mało, zajmuje całą dostępną kartę.
    const containerWidth = screenWidth - 70;
    const yAxisWidth = 35;
    const availableSvgWidth = containerWidth - yAxisWidth;

    const calculatedSvgWidth = Math.max(availableSvgWidth, data.length * 35);
    const stepX = data.length > 1 ? calculatedSvgWidth / (data.length - 1) : 0;

    const getX = (i: number) => i * stepX;
    const getY = (val: number) => PADDING_TOP + innerHeight - ((val - min) / range) * innerHeight;

    // Punkty do linii
    const points = data.map((val, i) => `${getX(i)},${getY(val)}`).join(' ');
    // Punkty do wypełnienia pod wykresem (zamykamy ścieżkę na dole)
    const polygonPoints = `${getX(0)},${CHART_HEIGHT} ${points} ${getX(data.length - 1)},${CHART_HEIGHT}`;

    return (
        <View style={{ width: containerWidth, height: CHART_HEIGHT, marginTop: 10, flexDirection: 'row' }}>

            {/* STATYCZNA OŚ Y (nie przewija się) */}
            <View style={{ width: yAxisWidth, height: CHART_HEIGHT, position: 'relative', zIndex: 2, backgroundColor: '#FFF' }}>
                <Text style={[styles.axisText, { top: PADDING_TOP - 8 }]}>{max.toFixed(0)}</Text>
                <Text style={[styles.axisText, { top: PADDING_TOP + innerHeight / 2 - 8 }]}>{((max + min) / 2).toFixed(0)}</Text>
                <Text style={[styles.axisText, { top: PADDING_TOP + innerHeight - 8 }]}>{min.toFixed(0)}</Text>
            </View>

            {/* PRZEWIJANY WYKRES */}
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ flex: 1 }}>
                <View style={{ width: calculatedSvgWidth, height: CHART_HEIGHT, position: 'relative' }}>
                    <Svg width="100%" height="100%">
                        <Defs>
                            {/* Definicja gradientu do wypełnienia */}
                            <LinearGradient id={`grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
                                <Stop offset="0%" stopColor={color} stopOpacity="0.3" />
                                <Stop offset="100%" stopColor={color} stopOpacity="0.0" />
                            </LinearGradient>
                        </Defs>

                        {/* POZIOME LINIE SIATKI */}
                        <Line x1="0" y1={PADDING_TOP} x2={calculatedSvgWidth} y2={PADDING_TOP} stroke="#F3F4F6" strokeDasharray="4" />
                        <Line x1="0" y1={PADDING_TOP + innerHeight / 2} x2={calculatedSvgWidth} y2={PADDING_TOP + innerHeight / 2} stroke="#F3F4F6" strokeDasharray="4" />
                        <Line x1="0" y1={PADDING_TOP + innerHeight} x2={calculatedSvgWidth} y2={PADDING_TOP + innerHeight} stroke="#E5E7EB" strokeWidth="2" />

                        {/* OBSZAR GRADIENTU */}
                        <Polygon points={polygonPoints} fill={`url(#grad-${color})`} />

                        {/* GŁÓWNA LINIA */}
                        <Polyline points={points} fill="none" stroke={color} strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" />

                        {/* PUNKTY POMIAROWE */}
                        {data.map((val, i) => {
                            const cx = getX(i);
                            const cy = getY(val);
                            const isSelected = activeIndex === i;
                            return (
                                <React.Fragment key={i}>
                                    {/* Mniejsze kropki jako standard, powiększają się po kliknięciu */}
                                    <Circle cx={cx} cy={cy} r={isSelected ? "6" : "3"} fill="#FFF" stroke={color} strokeWidth={isSelected ? "3" : "2"} />
                                    {/* Duży, niewidoczny obszar do klikania (Hitbox) */}
                                    <Circle cx={cx} cy={cy} r="20" fill="transparent" onPress={() => setActiveIndex(isSelected ? null : i)} />
                                </React.Fragment>
                            );
                        })}
                    </Svg>

                    {/* DYNAMICZNY DYMEK Z WYNIKIEM (Tooltip) */}
                    {activeIndex !== null && (
                        <View style={[styles.tooltip, {
                            left: Math.max(0, Math.min(getX(activeIndex) - 30, calculatedSvgWidth - 60)),
                            top: getY(data[activeIndex]) - 35
                        }]}>
                            <Text style={styles.tooltipText}>{data[activeIndex]} {unit}</Text>
                            <View style={[styles.tooltipArrow, { borderTopColor: color }]} />
                        </View>
                    )}
                </View>
            </ScrollView>
        </View>
    );
};

export const VitalsHistoryScreen = ({ route, navigation }: any) => {
    const insets = useSafeAreaInsets();
    const { patientData, token } = route.params || {};

    const [history, setHistory] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchHistory = async () => {
            if (!patientData?.id || !token) { setIsLoading(false); return; }
            try {
                // ZWIĘKSZONY LIMIT DO 100 POMIARÓW
                const API_URL = `http://10.0.2.2:8080/api/v1/vital-signs/patient/${patientData.id}?limit=100`;
                const response = await fetch(API_URL, {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                });
                const data = await response.json();
                const list = Array.isArray(data) ? data : (data.history || []);
                // Zostawiamy do 100 ostatnich, chronologicznie
                setHistory(list.slice(-100));
            } catch (err) {
                console.error(err);
            } finally {
                setIsLoading(false);
            }
        };
        fetchHistory();
    }, [patientData, token]);

    const getStats = (key: string, nested?: string) => {
        const values = history.map(item => nested ? item.measurements?.[key]?.[nested] : item.measurements?.[key]).filter(v => typeof v === 'number');
        return {
            min: values.length > 0 ? Math.min(...values) : '--',
            max: values.length > 0 ? Math.max(...values) : '--',
            latest: values.length > 0 ? values[values.length - 1] : '--',
            values: values
        };
    };

    const vitalsConfig = [
        { label: 'Tętno', key: 'heartRate', unit: 'bpm', color: '#EF4444' },
        { label: 'Ciśnienie (SYS)', key: 'bloodPressure', nested: 'systolic', unit: 'mmHg', color: '#8B5CF6' },
        { label: 'Saturacja (SpO2)', key: 'spO2', unit: '%', color: '#3B82F6' },
        { label: 'Temperatura', key: 'temperature', unit: '°C', color: '#F59E0B' },
    ];

    return (
        <View style={styles.containerRoot}>
            <View style={[styles.appBar, { paddingTop: insets.top + 16 }]}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <BackIcon />
                </TouchableOpacity>
                <Text style={styles.appBarTitle}>Historia zdrowia</Text>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent}>
                {isLoading ? (
                    <ActivityIndicator size="large" color="#3B82F6" style={{ marginTop: 50 }} />
                ) : history.length === 0 ? (
                    <Text style={styles.centerText}>Brak dostępnych danych.</Text>
                ) : (
                    <>
                        <View style={styles.card}>
                            <Text style={styles.cardTitle}>Najnowsze odczyty</Text>
                            {vitalsConfig.map((item, index) => {
                                const stats = getStats(item.key, item.nested);
                                return (
                                    <View key={index} style={styles.row}>
                                        <Text style={styles.label}>{item.label}</Text>
                                        <View style={styles.values}>
                                            <Text style={styles.latestValue}>{stats.latest} {item.unit}</Text>
                                            <Text style={styles.rangeText}>Min: {stats.min} | Max: {stats.max}</Text>
                                        </View>
                                    </View>
                                );
                            })}
                        </View>

                        <Text style={styles.sectionTitle}>Trend szczegółowy (do 100 pomiarów)</Text>
                        <Text style={styles.subtext}>* Przewiń wykres w poziomie i kliknij w punkt</Text>

                        {vitalsConfig.map((item, index) => {
                            const stats = getStats(item.key, item.nested);
                            return (
                                <View key={index} style={styles.chartCard}>
                                    <Text style={styles.chartLabel}>{item.label}</Text>
                                    <CustomLineChart data={stats.values} color={item.color} unit={item.unit} />
                                </View>
                            );
                        })}
                    </>
                )}
            </ScrollView>
        </View>
    );
};

const styles = StyleSheet.create({
    containerRoot: { flex: 1, backgroundColor: '#FAFAFA' },
    appBar: { flexDirection: 'row', alignItems: 'center', padding: 16, backgroundColor: '#FFF', borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
    backButton: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#F3F4F6', justifyContent: 'center', alignItems: 'center' },
    appBarTitle: { marginLeft: 16, fontSize: 20, fontWeight: '700', color: '#1F2937' },
    scrollContent: { padding: 20, paddingBottom: 60 },
    card: { backgroundColor: '#FFF', padding: 20, borderRadius: 20, elevation: 4, marginBottom: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8 },
    cardTitle: { fontSize: 18, fontWeight: '800', marginBottom: 15, color: '#1F2937' },
    row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
    label: { fontSize: 15, fontWeight: '600', color: '#6B7280' },
    values: { alignItems: 'flex-end' },
    latestValue: { fontSize: 17, fontWeight: '800', color: '#111827' },
    rangeText: { fontSize: 12, color: '#9CA3AF', marginTop: 2, fontWeight: '500' },
    sectionTitle: { fontSize: 18, fontWeight: '800', marginTop: 10, color: '#1F2937' },
    subtext: { fontSize: 13, color: '#9CA3AF', marginBottom: 15, fontWeight: '500' },
    chartCard: { backgroundColor: '#FFF', padding: 15, borderRadius: 20, marginBottom: 15, elevation: 3, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8 },
    chartLabel: { fontSize: 16, fontWeight: '800', color: '#374151', marginBottom: 5, marginLeft: 5 },
    noDataText: { fontSize: 13, color: '#9CA3AF', marginTop: 15, fontStyle: 'italic', textAlign: 'center' },
    centerText: { textAlign: 'center', marginTop: 50, color: '#6B7280' },

    // --- STYLE NOWEGO WYKRESU ---
    axisText: { position: 'absolute', fontSize: 11, color: '#9CA3AF', fontWeight: '700', left: 0 },
    tooltip: {
        position: 'absolute',
        backgroundColor: '#1F2937',
        paddingHorizontal: 10,
        paddingVertical: 6,
        borderRadius: 8,
        elevation: 5,
        shadowColor: '#000',
        shadowOpacity: 0.2,
        shadowRadius: 4,
        zIndex: 10,
        alignItems: 'center'
    },
    tooltipText: { fontSize: 13, fontWeight: 'bold', color: '#FFF' },
    tooltipArrow: {
        position: 'absolute',
        bottom: -5,
        width: 0,
        height: 0,
        borderLeftWidth: 5,
        borderRightWidth: 5,
        borderTopWidth: 5,
        borderLeftColor: 'transparent',
        borderRightColor: 'transparent',
    }
});