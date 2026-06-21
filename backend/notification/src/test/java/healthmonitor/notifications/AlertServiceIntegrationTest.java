package healthmonitor.notifications;

import healthmonitor.notifications.communication.AlertNotifier;
import healthmonitor.notifications.communication.MedicalStaffClient;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;
import healthmonitor.notifications.repository.AlertRepository;
import healthmonitor.notifications.sevice.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
public class AlertServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @MockitoBean
    private MedicalStaffClient medicalStaffClient;

    @MockitoBean
    private AlertNotifier alertNotifier;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
    }

    @Test
    void shouldProcessAndSaveAlertAndNotifyDoctors() {
        AlertEventMessage eventMessage = new AlertEventMessage();
        eventMessage.setPatientId("pat-1");
        eventMessage.setRiskScore(0.9);
        eventMessage.setSeverity("CRITICAL");
        eventMessage.setMessage("Heart rate spike");
        eventMessage.setTimestamp(LocalDateTime.now());

        when(medicalStaffClient.getDoctorIdsForPatient("pat-1")).thenReturn(List.of("doc-1", "doc-2"));

        alertService.processAlert(eventMessage);

        List<Alert> savedAlerts = alertRepository.findAll();
        assertEquals(1, savedAlerts.size());
        Alert savedAlert = savedAlerts.getFirst();
        assertNotNull(savedAlert.getAlertId());
        assertEquals("pat-1", savedAlert.getPatientId());
        assertEquals("CRITICAL", savedAlert.getSeverity());

        verify(medicalStaffClient, times(1)).getDoctorIdsForPatient("pat-1");
        verify(alertNotifier, times(1)).notifyDoctors(any(Alert.class), eq(List.of("doc-1", "doc-2")));
    }

    @Test
    void shouldSaveAlertWhenNoDoctorsAssigned() {
        AlertEventMessage eventMessage = new AlertEventMessage();
        eventMessage.setPatientId("pat-no-docs");
        eventMessage.setRiskScore(0.5);
        eventMessage.setMessage("Any message");
        eventMessage.setTimestamp(LocalDateTime.now());

        when(medicalStaffClient.getDoctorIdsForPatient("pat-no-docs")).thenReturn(List.of());

        alertService.processAlert(eventMessage);

        List<Alert> savedAlerts = alertRepository.findAll();
        assertEquals(1, savedAlerts.size());
        assertEquals("pat-no-docs", savedAlerts.getFirst().getPatientId());

        verify(medicalStaffClient, times(1)).getDoctorIdsForPatient("pat-no-docs");
        verify(alertNotifier, times(0)).notifyDoctors(any(), any());
    }


    @Test
    void shouldGetUnreadNotificationsForPatient() {
        String patientId = "pat-2";
        alertRepository.save(createAlert(patientId, false, LocalDateTime.now()));
        alertRepository.save(createAlert(patientId, true, LocalDateTime.now().minusDays(1)));
        alertRepository.save(createAlert(patientId, false, LocalDateTime.now().minusHours(1)));

        List<AlertDto> unreadAlerts = alertService.getNotificationsForPatient(patientId);

        assertEquals(2, unreadAlerts.size());
        assertTrue(unreadAlerts.stream().noneMatch(AlertDto::isRead));
        assertTrue(unreadAlerts.getFirst().getTimestamp().isAfter(unreadAlerts.getLast().getTimestamp()));
    }

    @Test
    void shouldReturnEmptyListWhenNoUnreadNotificationsForPatient() {
        String patientId = "pat-empty-unread";
        alertRepository.save(createAlert(patientId, true, LocalDateTime.now()));

        List<AlertDto> unreadAlerts = alertService.getNotificationsForPatient(patientId);

        assertTrue(unreadAlerts.isEmpty());
    }

    @Test
    void shouldGetAllNotificationsForPatient() {
        String patientId = "pat-3";
        alertRepository.save(createAlert(patientId, false, LocalDateTime.now()));
        alertRepository.save(createAlert(patientId, true, LocalDateTime.now().minusDays(1)));

        alertRepository.save(createAlert("pat-other", false, LocalDateTime.now()));

        List<AlertDto> allAlerts = alertService.getAllNotifications(patientId);

        assertEquals(2, allAlerts.size());
        assertTrue(allAlerts.stream().allMatch(a -> a.getPatientId().equals(patientId)));
    }

    @Test
    void shouldReturnEmptyListWhenPatientHasNoNotifications() {
        List<AlertDto> allAlerts = alertService.getAllNotifications("pat-ghost");

        assertTrue(allAlerts.isEmpty());
    }

    private Alert createAlert(String patientId, boolean isRead, LocalDateTime timestamp) {
        return Alert.builder()
                .patientId(patientId)
                .riskScore(0.5)
                .severity("MEDIUM")
                .message("Test message")
                .isRead(isRead)
                .timestamp(timestamp)
                .build();
    }
}