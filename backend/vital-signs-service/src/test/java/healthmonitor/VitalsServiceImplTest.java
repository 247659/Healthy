package healthmonitor;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.dto.VitalThresholdDto;
import healthmonitor.vitals.event.VitalThresholdEvent;
import healthmonitor.vitals.mapper.VitalThresholdMapper;
import healthmonitor.vitals.model.VitalThreshold;
import healthmonitor.vitals.repository.VitalThresholdRepository;
import healthmonitor.vitals.service.VitalsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalsServiceImplTest {

    @Mock
    private VitalThresholdMapper vitalThresholdMapper;

    @Mock
    private VitalThresholdRepository vitalThresholdRepository;

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private WriteApi writeApi;

    @Mock
    private WriteApiBlocking writeApiBlocking;

    @Mock
    private QueryApi queryApi;

    private VitalsServiceImpl vitalsService;

    @BeforeEach
    void setUp() {
        // Konfigurujemy zachowanie mocka InfluxDB, ponieważ konstruktor VitalsServiceImpl
        // od razu pobiera z niego instancję WriteApi
        when(influxDBClient.makeWriteApi()).thenReturn(writeApi);

        // Inicjalizacja serwisu (zamiast @InjectMocks, żeby mieć pełną kontrolę)
        vitalsService = new VitalsServiceImpl(vitalThresholdMapper, vitalThresholdRepository, influxDBClient);

        // Ustawiamy prywatne pola zasilane normalnie przez @Value w Springu
        ReflectionTestUtils.setField(vitalsService, "bucket", "test_bucket");
        ReflectionTestUtils.setField(vitalsService, "organization", "test_org");
    }

    @Test
    void saveThreshold_ShouldSaveNewThresholdEvent() {
        // given
        VitalThresholdEvent event = new VitalThresholdEvent("patient-1");

        // when
        vitalsService.saveThreshold(event);

        // then
        ArgumentCaptor<VitalThreshold> captor = ArgumentCaptor.forClass(VitalThreshold.class);
        verify(vitalThresholdRepository, times(1)).save(captor.capture());
        assertEquals("patient-1", captor.getValue().getPatientId());
    }

    @Test
    void getByPatientId_ShouldReturnThresholdDto_WhenExists() {
        // given
        String patientId = "patient-1";
        VitalThreshold threshold = new VitalThreshold();
        threshold.setPatientId(patientId);

        VitalThresholdDto dto = new VitalThresholdDto(80, 70, 95, 120, BigDecimal.valueOf(36.6));

        when(vitalThresholdRepository.findById(patientId)).thenReturn(Optional.of(threshold));
        when(vitalThresholdMapper.toDto(threshold)).thenReturn(dto);

        // when
        VitalThresholdDto result = vitalsService.getByPatientId(patientId);

        // then
        assertNotNull(result);
        assertEquals(120, result.systolicBp());
        verify(vitalThresholdRepository, times(1)).findById(patientId);
    }

    @Test
    void getByPatientId_ShouldThrowException_WhenNotFound() {
        // given
        String patientId = "patient-1";
        when(vitalThresholdRepository.findById(patientId)).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vitalsService.getByPatientId(patientId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Patient's vital threshold not found", exception.getReason());
        verify(vitalThresholdMapper, never()).toDto(any());
    }

    @Test
    void update_ShouldUpdateThresholdAndReturnDto() {
        // given
        String patientId = "patient-1";
        VitalThresholdDto requestDto = new VitalThresholdDto(90, 80, 98, 140, BigDecimal.valueOf(37.0));
        VitalThreshold entity = new VitalThreshold();

        when(vitalThresholdMapper.toEntity(requestDto)).thenReturn(entity);
        when(vitalThresholdRepository.save(entity)).thenReturn(entity);
        when(vitalThresholdMapper.toDto(entity)).thenReturn(requestDto);

        // when
        VitalThresholdDto result = vitalsService.update(patientId, requestDto);

        // then
        assertNotNull(result);
        assertEquals(140, result.systolicBp());
        assertEquals(patientId, entity.getPatientId()); // Upewniamy się, że ID z URL zostało przypisane do encji

        verify(vitalThresholdMapper, times(1)).toEntity(requestDto);
        verify(vitalThresholdRepository, times(1)).save(entity);
    }

    @Test
    void processAndSaveVitals_ShouldWritePointToInfluxDb() {
        // given
        VitalSignsDto dto = createSampleVitalSignsDto("patient-1");
        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApiBlocking);

        // when
        vitalsService.processAndSaveVitals(dto);

        // then
        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        verify(influxDBClient, times(1)).getWriteApiBlocking();
        verify(writeApiBlocking, times(1)).writePoint(eq("test_bucket"), eq("test_org"), pointCaptor.capture());

        Point capturedPoint = pointCaptor.getValue();
        assertTrue(capturedPoint.toLineProtocol().contains("patient_id=patient-1"));
        assertTrue(capturedPoint.toLineProtocol().contains("heart_rate=80"));
    }

    @Test
    void processAndSaveVitalsBatch_ShouldWritePointsUsingAsyncApi() {
        // given
        VitalSignsDto dto1 = createSampleVitalSignsDto("patient-1");
        VitalSignsDto dto2 = createSampleVitalSignsDto("patient-2");
        List<VitalSignsDto> batch = List.of(dto1, dto2);

        // when
        vitalsService.processAndSaveVitalsBatch(batch);

        // then
        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);

        // Upewniamy się, że metoda writePoint na asynchronicznym API została wywołana dwa razy
        verify(writeApi, times(2)).writePoint(eq("test_bucket"), eq("test_org"), pointCaptor.capture());

        List<Point> capturedPoints = pointCaptor.getAllValues();
        assertEquals(2, capturedPoints.size());
        assertTrue(capturedPoints.get(0).toLineProtocol().contains("patient_id=patient-1"));
        assertTrue(capturedPoints.get(1).toLineProtocol().contains("patient_id=patient-2"));
    }

    @Test
    void getPatientHistory_ShouldReturnEmptyList_WhenQueryFailsOrNoData() {
        // given
        String patientId = "patient-1";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        // Symulacja zwrócenia pustej listy z Flux (brak wyników)
        when(queryApi.query(anyString(), eq("test_org"))).thenReturn(List.of());

        // when
        List<VitalSignsDto> result = vitalsService.getPatientHistory(patientId, from, to);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(queryApi, times(1)).query(anyString(), eq("test_org"));
    }

    // Metoda pomocnicza do budowania poprawnie zwalidowanego DTO
    private VitalSignsDto createSampleVitalSignsDto(String patientId) {
        VitalSignsDto dto = new VitalSignsDto();
        dto.setPatientId(patientId);
        dto.setTimestamp(Instant.now().toString());

        VitalSignsDto.BloodPressure bp = new VitalSignsDto.BloodPressure();
        bp.setSystolic(120);
        bp.setDiastolic(80);

        VitalSignsDto.Measurements measurements = new VitalSignsDto.Measurements();
        measurements.setHeartRate(80);
        measurements.setTemperature(36.6);
        measurements.setSpO2(98);
        measurements.setBloodPressure(bp);

        dto.setMeasurements(measurements);
        return dto;
    }

    @Test
    void getPatientHistory_ShouldReturnMappedHistory_WhenAllFieldsArePresent() {
        // given
        String patientId = "patient-1";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        Instant recordTime = Instant.now().minusSeconds(1800);

        // Tworzenie mocków dla klas z biblioteki InfluxDB
        FluxTable mockTable = mock(FluxTable.class);
        FluxRecord mockRecord = mock(FluxRecord.class);

        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("test_org"))).thenReturn(List.of(mockTable));
        when(mockTable.getRecords()).thenReturn(List.of(mockRecord));

        // Symulacja danych zwracanych z rekordu InfluxDB
        when(mockRecord.getTime()).thenReturn(recordTime);
        when(mockRecord.getValueByKey("heart_rate")).thenReturn(75);
        when(mockRecord.getValueByKey("temperature")).thenReturn(36.6);
        when(mockRecord.getValueByKey("spO2")).thenReturn(98);
        when(mockRecord.getValueByKey("systolic_bp")).thenReturn(120);
        when(mockRecord.getValueByKey("diastolic_bp")).thenReturn(80);

        // when
        List<VitalSignsDto> history = vitalsService.getPatientHistory(patientId, from, to);

        // then
        assertNotNull(history);
        assertEquals(1, history.size());

        VitalSignsDto dto = history.get(0);
        assertEquals(patientId, dto.getPatientId());
        assertEquals(recordTime.toString(), dto.getTimestamp());

        assertNotNull(dto.getMeasurements());
        assertEquals(75, dto.getMeasurements().getHeartRate());
        assertEquals(36.6, dto.getMeasurements().getTemperature());
        assertEquals(98, dto.getMeasurements().getSpO2());

        assertNotNull(dto.getMeasurements().getBloodPressure());
        assertEquals(120, dto.getMeasurements().getBloodPressure().getSystolic());
        assertEquals(80, dto.getMeasurements().getBloodPressure().getDiastolic());

        // Weryfikacja, czy poprawnie sformowano zapytanie
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryApi).query(queryCaptor.capture(), eq("test_org"));
        String executedQuery = queryCaptor.getValue();
        assertTrue(executedQuery.contains("from(bucket: \"test_bucket\")"));
        assertTrue(executedQuery.contains(patientId));
    }

    @Test
    void getPatientHistory_ShouldHandleMissingFieldsGracefully() {
        // given
        String patientId = "patient-1";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        Instant recordTime = Instant.now().minusSeconds(1800);

        FluxTable mockTable = mock(FluxTable.class);
        FluxRecord mockRecord = mock(FluxRecord.class);

        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("test_org"))).thenReturn(List.of(mockTable));
        when(mockTable.getRecords()).thenReturn(List.of(mockRecord));

        // Zwracamy tylko tętno. Zostawiamy null dla pozostałych parametrów (temperatura, ciśnienie, spO2)
        when(mockRecord.getTime()).thenReturn(recordTime);
        when(mockRecord.getValueByKey("heart_rate")).thenReturn(85);
        when(mockRecord.getValueByKey("temperature")).thenReturn(null);
        when(mockRecord.getValueByKey("spO2")).thenReturn(null);
        when(mockRecord.getValueByKey("systolic_bp")).thenReturn(null);
        when(mockRecord.getValueByKey("diastolic_bp")).thenReturn(null);

        // when
        List<VitalSignsDto> history = vitalsService.getPatientHistory(patientId, from, to);

        // then
        assertNotNull(history);
        assertEquals(1, history.size());

        VitalSignsDto dto = history.get(0);
        assertEquals(patientId, dto.getPatientId());

        // Upewniamy się, że tętno zostało zmapowane, a reszta pozostała na wartościach domyślnych (0 / 0.0)
        assertEquals(85, dto.getMeasurements().getHeartRate());
        assertEquals(0.0, dto.getMeasurements().getTemperature());
        assertEquals(0, dto.getMeasurements().getSpO2());

        assertNotNull(dto.getMeasurements().getBloodPressure());
        assertEquals(0, dto.getMeasurements().getBloodPressure().getSystolic());
        assertEquals(0, dto.getMeasurements().getBloodPressure().getDiastolic());
    }

    @Test
    void getPatientHistory_ShouldReturnEmptyList_WhenNoRecordsFound() {
        // given
        String patientId = "patient-1";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        // Zwracamy pustą listę tabel z InfluxDB
        when(queryApi.query(anyString(), eq("test_org"))).thenReturn(List.of());

        // when
        List<VitalSignsDto> history = vitalsService.getPatientHistory(patientId, from, to);

        // then
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void getPatientHistory_ShouldCatchExceptionAndReturnEmptyList_WhenDatabaseFails() {
        // given
        String patientId = "patient-1";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        // Symulacja błędu połączenia lub błędu składni po stronie InfluxDB
        when(queryApi.query(anyString(), eq("test_org"))).thenThrow(new RuntimeException("InfluxDB connection timeout"));

        // when
        // Metoda nie powinna rzucić wyjątku w górę, ponieważ jest on obsługiwany w bloku try-catch
        List<VitalSignsDto> history = vitalsService.getPatientHistory(patientId, from, to);

        // then
        assertNotNull(history);
        assertTrue(history.isEmpty()); // Powinna zwrócić to, co zdążyła zainicjalizować (czyli pustą ArrayList)
    }
}