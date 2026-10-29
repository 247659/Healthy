package healthmonitor.notifications;

import healthmonitor.notifications.communication.AlertNotifier;
import healthmonitor.notifications.communication.MedicalStaffClient;
import healthmonitor.notifications.exception.MarkingAsReadException;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;
import healthmonitor.notifications.repository.AlertRepository;
import healthmonitor.notifications.sevice.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

    @MockitoBean
    private JwtDecoder jwtDecoder;

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

        when(medicalStaffClient.getDoctorIdsForPatient("pat-1")).thenReturn(List.of("doc-1"));

        alertService.processAlert(eventMessage);

        List<Alert> savedAlerts = alertRepository.findAll();
        assertEquals(2, savedAlerts.size());

        Alert patientAlert = savedAlerts.stream()
                .filter(a -> "pat-1".equals(a.getDestination()))
                .findFirst()
                .orElseThrow();

        Alert doctorAlert = savedAlerts.stream()
                .filter(a -> "doc-1".equals(a.getDestination()))
                .findFirst()
                .orElseThrow();

        assertNotNull(patientAlert.getAlertId());
        assertNotNull(doctorAlert.getAlertId());
        assertEquals("pat-1", patientAlert.getPatientId());
        assertEquals("CRITICAL", patientAlert.getSeverity());
        assertEquals("pat-1", patientAlert.getDestination());
        assertEquals("pat-1", doctorAlert.getPatientId());
        assertEquals("CRITICAL", doctorAlert.getSeverity());
        assertEquals("doc-1", doctorAlert.getDestination());

        verify(medicalStaffClient, times(1)).getDoctorIdsForPatient("pat-1");
        verify(alertNotifier, times(1)).notifyDoctor(
                argThat(dto ->
                        dto != null &&
                                "pat-1".equals(dto.getPatientId()) &&
                                "CRITICAL".equals(dto.getSeverity()) &&
                                "Heart rate spike".equals(dto.getMessage()) &&
                                dto.getRiskScore() == 0.9
                ),
                eq("doc-1")
        );
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
        verify(alertNotifier, times(0)).notifyDoctor(any(), any());
    }

    @Test
    void shouldGetUnreadNotificationsForPatient() {
        String patientId = "pat-2";
        alertRepository.save(createAlert(patientId, false, LocalDateTime.now(), "id"));
        alertRepository.save(createAlert(patientId, true, LocalDateTime.now().minusDays(1), "id"));
        alertRepository.save(createAlert(patientId, false, LocalDateTime.now().minusHours(1), "id"));

        List<AlertDto> unreadAlerts = alertService.getNotificationsForPatient(patientId, "id");

        assertEquals(2, unreadAlerts.size());
        assertTrue(unreadAlerts.stream().noneMatch(AlertDto::isRead));
        assertTrue(unreadAlerts.getFirst().getTimestamp().isAfter(unreadAlerts.getLast().getTimestamp()));
    }

    @Test
    void shouldReturnEmptyListWhenNoUnreadNotificationsForPatient() {
        String patientId = "pat-empty-unread";
        alertRepository.save(createAlert(patientId, true, LocalDateTime.now(), "id"));

        List<AlertDto> unreadAlerts = alertService.getNotificationsForPatient(patientId, "id");

        assertTrue(unreadAlerts.isEmpty());
    }

    @Test
    void shouldGetAllNotificationsForPatient() {
        String patientId = "pat-3";
        alertRepository.save(createAlert(patientId, false, LocalDateTime.now(), "id"));
        alertRepository.save(createAlert(patientId, true, LocalDateTime.now().minusDays(1), "id"));
        alertRepository.save(createAlert("pat-other", false, LocalDateTime.now(), "id"));

        List<AlertDto> allAlerts = alertService.getAllNotifications(patientId, "id", 0, 10).getContent();

        assertEquals(2, allAlerts.size());
        assertTrue(allAlerts.stream().allMatch(a -> a.getPatientId().equals(patientId)));
    }

    @Test
    void shouldReturnEmptyListWhenPatientHasNoNotifications() {
        List<AlertDto> allAlerts = alertService.getAllNotifications("pat-ghost", "id", 0, 10).getContent();

        assertTrue(allAlerts.isEmpty());
    }

    @Test
    void shouldMarkAlertsAsRead() {
        String patientId = "pat-read";
        Alert first = alertRepository.save(createAlert(patientId, false, LocalDateTime.now(), "user-1"));
        Alert second = alertRepository.save(createAlert(patientId, false, LocalDateTime.now().minusMinutes(1), "user-1"));
        alertRepository.save(createAlert(patientId, false, LocalDateTime.now().minusMinutes(2), "other-user"));

        alertService.markAsRead(List.of(first.getAlertId(), second.getAlertId()), "user-1");

        List<Alert> updated = alertRepository.findByPatientIdAndDestination(patientId, "user-1", null).getContent();
        assertEquals(2, updated.size());
        assertTrue(updated.stream().allMatch(Alert::isRead));
    }

    @Test
    void shouldThrowExceptionWhenMarkingAlertsAsReadForUnknownIds() {
        assertThrows(MarkingAsReadException.class,
                () -> alertService.markAsRead(List.of("missing-alert-id"), "user-1"));
    }

    private Alert createAlert(String patientId, boolean isRead, LocalDateTime timestamp, String destination) {
        return Alert.builder()
                .patientId(patientId)
                .riskScore(0.5)
                .severity("MEDIUM")
                .message("Test message")
                .isRead(isRead)
                .timestamp(timestamp)
                .destination(destination)
                .build();
    }
}