package healthmonitor.notifications;

import healthmonitor.notifications.communication.AlertNotifier;
import healthmonitor.notifications.communication.MedicalStaffClient;
import healthmonitor.notifications.exception.MarkingAsReadException;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;
import healthmonitor.notifications.repository.AlertRepository;
import healthmonitor.notifications.sevice.AlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private MedicalStaffClient medicalStaffClient;

    @Mock
    private AlertNotifier alertNotifier;

    @InjectMocks
    private AlertServiceImpl alertService;

    private AlertEventMessage eventMessage;
    private Alert savedAlert1;
    private Alert savedAlert2;

    @BeforeEach
    void setUp() {
        eventMessage = new AlertEventMessage();
        eventMessage.setPatientId("patient-123");
        eventMessage.setRiskScore(0.85);
        eventMessage.setSeverity("HIGH");
        eventMessage.setMessage("Abnormal heart rate detected");
        eventMessage.setDetails(List.of("Heart rate > 120 bpm"));
        eventMessage.setRecommendation("Check patient immediately");
        eventMessage.setForecastNote("Patient condition might worsen");
        eventMessage.setMethod("ML-Model-v1");
        eventMessage.setTimestamp(LocalDateTime.now());

        savedAlert1 = new Alert();
        savedAlert1.setAlertId("alert-uuid-1234");
        savedAlert1.setPatientId(eventMessage.getPatientId());
        savedAlert1.setRiskScore(eventMessage.getRiskScore());
        savedAlert1.setSeverity(eventMessage.getSeverity());
        savedAlert1.setMessage(eventMessage.getMessage());
        savedAlert1.setDetails(eventMessage.getDetails());
        savedAlert1.setTimestamp(eventMessage.getTimestamp());
        savedAlert1.setRead(false);

        savedAlert2 = new Alert();
        savedAlert2.setAlertId("alert-uuid-2222");
        savedAlert2.setPatientId(eventMessage.getPatientId());
        savedAlert2.setRiskScore(eventMessage.getRiskScore());
        savedAlert2.setSeverity(eventMessage.getSeverity());
        savedAlert2.setMessage(eventMessage.getMessage());
        savedAlert2.setDetails(eventMessage.getDetails());
        savedAlert2.setTimestamp(eventMessage.getTimestamp());
        savedAlert2.setRead(false);
    }

    @Test
    void processAlert_ShouldSaveAlertAndNotifyDoctors_WhenDoctorsExist() {
        String doctorId = "doc-1";
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert1, savedAlert2);
        when(medicalStaffClient.getDoctorIdsForPatient("patient-123")).thenReturn(List.of(doctorId));

        alertService.processAlert(eventMessage);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(2)).save(alertCaptor.capture());

        List<Alert> capturedAlerts = alertCaptor.getAllValues();
        assertEquals(2, capturedAlerts.size());
        assertEquals("patient-123", capturedAlerts.get(0).getPatientId());
        assertEquals("patient-123", capturedAlerts.get(1).getPatientId());
        assertEquals("HIGH", capturedAlerts.get(0).getSeverity());
        assertEquals("HIGH", capturedAlerts.get(1).getSeverity());

        verify(medicalStaffClient, times(1)).getDoctorIdsForPatient("patient-123");
        verify(alertNotifier, times(1)).notifyDoctor(
                argThat(dto ->
                        dto != null &&
                                "patient-123".equals(dto.getPatientId()) &&
                                "HIGH".equals(dto.getSeverity()) &&
                                "Abnormal heart rate detected".equals(dto.getMessage()) &&
                                dto.getDetails() != null &&
                                dto.getDetails().contains("Heart rate > 120 bpm")
                ),
                eq(doctorId)
        );
    }

    @Test
    void processAlert_ShouldSaveAlertAndNotNotify_WhenNoDoctorsAssigned() {
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert1);
        when(medicalStaffClient.getDoctorIdsForPatient("patient-123")).thenReturn(Collections.emptyList());

        alertService.processAlert(eventMessage);

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(medicalStaffClient, times(1)).getDoctorIdsForPatient("patient-123");
        verify(alertNotifier, never()).notifyDoctor(any(), any());
    }

    @Test
    void processAlert_ShouldSaveAlertEvenIfNotificationFails() {
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert1);
        when(medicalStaffClient.getDoctorIdsForPatient("patient-123"))
                .thenThrow(new RuntimeException("Medical Staff Service Unavailable"));

        assertDoesNotThrow(() -> alertService.processAlert(eventMessage));

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(alertNotifier, never()).notifyDoctor(any(), any());
    }

    @Test
    void getNotificationsForPatient_ShouldReturnMappedUnreadAlerts() {
        String patientId = "patient-123";
        String userId = "user-456";

        when(alertRepository.findByPatientIdAndDestinationAndIsReadFalseOrderByTimestampDesc(patientId, userId))
                .thenReturn(List.of(savedAlert1));

        List<AlertDto> result = alertService.getNotificationsForPatient(patientId, userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("alert-uuid-1234", result.getFirst().getAlertId());
        assertEquals("patient-123", result.getFirst().getPatientId());
        assertFalse(result.getFirst().isRead());

        verify(alertRepository, times(1))
                .findByPatientIdAndDestinationAndIsReadFalseOrderByTimestampDesc(patientId, userId);
    }

    @Test
    void getAllNotifications_ShouldReturnAllMappedAlerts() {
        String patientId = "patient-123";
        String userId = "user-456";
        Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());

        Alert oldReadAlert = new Alert();
        oldReadAlert.setAlertId("old-uuid");
        oldReadAlert.setPatientId(patientId);
        oldReadAlert.setRead(true);

        when(alertRepository.findByPatientIdAndDestination(patientId, userId, pageable))
                .thenReturn(new PageImpl<>(List.of(savedAlert1, oldReadAlert)));

        List<AlertDto> result = alertService.getAllNotifications(patientId, userId, 0, 10).getContent();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertFalse(result.get(0).isRead());
        assertTrue(result.get(1).isRead());

        verify(alertRepository, times(1)).findByPatientIdAndDestination(patientId, userId, pageable);
    }

    @Test
    void markAsRead_ShouldMarkProvidedAlertsAsRead() {
        String userId = "user-456";
        List<String> ids = List.of("alert-uuid-1234", "alert-uuid-2222");

        Alert first = new Alert();
        first.setAlertId(ids.get(0));
        first.setDestination(userId);
        first.setRead(false);

        Alert second = new Alert();
        second.setAlertId(ids.get(1));
        second.setDestination(userId);
        second.setRead(false);

        when(alertRepository.findAllByAlertIdAndDestination(ids, userId)).thenReturn(List.of(first, second));

        alertService.markAsRead(ids, userId);

        assertTrue(first.isRead());
        assertTrue(second.isRead());
        verify(alertRepository, times(1)).saveAll(List.of(first, second));
    }

    @Test
    void markAsRead_ShouldThrowException_WhenNoAlertsMatch() {
        String userId = "user-456";
        List<String> ids = List.of("missing-alert-id");

        when(alertRepository.findAllByAlertIdAndDestination(ids, userId)).thenReturn(Collections.emptyList());

        assertThrows(MarkingAsReadException.class, () -> alertService.markAsRead(ids, userId));
        verify(alertRepository, never()).saveAll(anyList());
    }
}