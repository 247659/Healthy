package healthmonitor;

import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.dto.VitalThresholdDto;
import healthmonitor.vitals.event.VitalThresholdEvent;
import healthmonitor.vitals.repository.VitalThresholdRepository;
import healthmonitor.vitals.service.VitalsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class VitalsServiceIntegrationTest {

    @Container
    static InfluxDBContainer<?> influxDB = new InfluxDBContainer<>(DockerImageName.parse("influxdb:2.7"))
            .withOrganization("test_org")
            .withBucket("test_bucket")
            .withAdminToken("my-super-secret-token");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("influxdb.url", influxDB::getUrl);
        registry.add("influxdb.bucket", () -> "test_bucket");
        registry.add("influxdb.org", () -> "test_org");
        registry.add("influxdb.token", () -> "my-super-secret-token");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VitalsService vitalsService;

    @Autowired
    private VitalThresholdRepository thresholdRepository;

    @BeforeEach
    void setUp() {
        thresholdRepository.deleteAll();
    }

    @Test
    void shouldProcessAndSaveVitals() throws InterruptedException {
        VitalSignsDto dto = createSampleVitalSigns("patient-123");
        vitalsService.processAndSaveVitals(dto);
        Thread.sleep(1000);

        List<VitalSignsDto> history = vitalsService.getPatientHistory("patient-123", Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS));
        assertFalse(history.isEmpty());
    }

    @Test
    void shouldProcessBatchVitals() throws InterruptedException {
        VitalSignsDto dto1 = createSampleVitalSigns("patient-batch");
        VitalSignsDto dto2 = createSampleVitalSigns("patient-batch");

        dto2.setTimestamp(Instant.parse(dto1.getTimestamp()).plus(1, ChronoUnit.SECONDS).toString());

        List<VitalSignsDto> batch = List.of(dto1, dto2);

        vitalsService.processAndSaveVitalsBatch(batch);

        Thread.sleep(2000);

        List<VitalSignsDto> history = vitalsService.getPatientHistory(
                "patient-batch",
                Instant.now().minus(1, ChronoUnit.MINUTES),
                Instant.now().plus(1, ChronoUnit.MINUTES)
        );

        assertEquals(2, history.size());
    }

    @Test
    void shouldSaveAndGetThreshold() {
        // Test saveThreshold (event)
        vitalsService.saveThreshold(new VitalThresholdEvent("pat-999"));

        // Test getByPatientId
        VitalThresholdDto threshold = vitalsService.getByPatientId("pat-999");
        assertNotNull(threshold);
        assertEquals(120, threshold.systolicBp());
    }

    @Test
    void shouldUpdateThreshold() {
        vitalsService.saveThreshold(new VitalThresholdEvent("pat-888"));
        VitalThresholdDto updateReq = new VitalThresholdDto(90, 80, 96, 130, BigDecimal.valueOf(36.8));

        VitalThresholdDto updated = vitalsService.update("pat-888", updateReq);
        assertEquals(130, updated.systolicBp());
    }

    private VitalSignsDto createSampleVitalSigns(String patientId) {
        VitalSignsDto dto = new VitalSignsDto();
        dto.setPatientId(patientId);
        dto.setTimestamp(Instant.now().toString());
        VitalSignsDto.Measurements m = new VitalSignsDto.Measurements();
        m.setHeartRate(80);
        m.setTemperature(36.6);
        m.setSpO2(98);
        VitalSignsDto.BloodPressure bp = new VitalSignsDto.BloodPressure();
        bp.setSystolic(120);
        bp.setDiastolic(80);
        m.setBloodPressure(bp);
        dto.setMeasurements(m);
        return dto;
    }
}