package healthmonitor.notifications;

import healthmonitor.notifications.communication.AlertNotifier;
import healthmonitor.notifications.communication.MedicalStaffClient;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private Alert savedAlert;

    @BeforeEach
    void setUp() {
        // Przygotowanie przykładowego komunikatu (np. z królika)
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

        // Przygotowanie encji zwracanej przez mocka bazy danych
        savedAlert = new Alert();
        savedAlert.setAlertId("alert-uuid-1234");
        savedAlert.setPatientId(eventMessage.getPatientId());
        savedAlert.setRiskScore(eventMessage.getRiskScore());
        savedAlert.setSeverity(eventMessage.getSeverity());
        savedAlert.setMessage(eventMessage.getMessage());
        savedAlert.setDetails(eventMessage.getDetails());
        savedAlert.setTimestamp(eventMessage.getTimestamp());
        savedAlert.setRead(false);
    }

    @Test
    void processAlert_ShouldSaveAlertAndNotifyDoctors_WhenDoctorsExist() {
        // given
        List<String> doctorIds = List.of("doc-1", "doc-2");
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);
        when(medicalStaffClient.getDoctorIdsForPatient("patient-123")).thenReturn(doctorIds);

        // when
        alertService.processAlert(eventMessage);

        // then
        // 1. Sprawdzamy zapis do bazy - przechwytujemy encję Alert
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertCaptor.capture());

        Alert capturedAlert = alertCaptor.getValue();
        assertEquals("patient-123", capturedAlert.getPatientId());
        assertEquals(0.85, capturedAlert.getRiskScore());
        assertEquals("HIGH", capturedAlert.getSeverity());
        assertEquals("Abnormal heart rate detected", capturedAlert.getMessage());
        assertEquals("Check patient immediately", capturedAlert.getRecommendation());
        assertFalse(capturedAlert.isRead()); // Domyślnie false

        // 2. Sprawdzamy, czy system pobrał lekarzy i wysłał powiadomienie
        verify(medicalStaffClient, times(1)).getDoctorIdsForPatient("patient-123");
        verify(alertNotifier, times(1)).notifyDoctors(savedAlert, doctorIds);
    }

    @Test
    void processAlert_ShouldSaveAlertAndNotNotify_WhenNoDoctorsAssigned() {
        // given
        // Symulacja przypadku, w którym do pacjenta nie przypisano żadnych lekarzy (pusta lista)
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);
        when(medicalStaffClient.getDoctorIdsForPatient("patient-123")).thenReturn(Collections.emptyList());

        // when
        alertService.processAlert(eventMessage);

        // then
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(medicalStaffClient, times(1)).getDoctorIdsForPatient("patient-123");
        // Upewniamy się, że nie wysłano powiadomień przez WebSockety, bo lista była pusta
        verify(alertNotifier, never()).notifyDoctors(any(), any());
    }

    @Test
    void processAlert_ShouldSaveAlertEvenIfNotificationFails() {
        // given
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);
        // Symulacja błędu klienta (np. serwer Medical Staff jest wyłączony, leci wyjątek)
        when(medicalStaffClient.getDoctorIdsForPatient("patient-123"))
                .thenThrow(new RuntimeException("Medical Staff Service Unavailable"));

        // when
        // Zgodnie z kodem w AlertServiceImpl: blok try-catch wokół sendNotificationToDoctors
        // łapie wyjątek i pozwala aplikacji działać dalej
        assertDoesNotThrow(() -> alertService.processAlert(eventMessage));

        // then
        // Upewniamy się, że mimo błędu powiadomień, alert ZADZIAŁAŁ w kontekście zapisu do bazy
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(alertNotifier, never()).notifyDoctors(any(), any());
    }

    @Test
    void getNotificationsForPatient_ShouldReturnMappedUnreadAlerts() {
        // given
        String patientId = "patient-123";
        when(alertRepository.findByPatientIdAndIsReadFalseOrderByTimestampDesc(patientId))
                .thenReturn(List.of(savedAlert));

        // when
        List<AlertDto> result = alertService.getNotificationsForPatient(patientId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());

        AlertDto dto = result.get(0);
        assertEquals("alert-uuid-1234", dto.getAlertId());
        assertEquals("patient-123", dto.getPatientId());
        assertEquals(0.85, dto.getRiskScore());
        assertEquals("HIGH", dto.getSeverity());
        assertFalse(dto.isRead());

        verify(alertRepository, times(1)).findByPatientIdAndIsReadFalseOrderByTimestampDesc(patientId);
    }

    @Test
    void getAllNotifications_ShouldReturnAllMappedAlerts() {
        // given
        String patientId = "patient-123";

        Alert oldReadAlert = new Alert();
        oldReadAlert.setAlertId("old-uuid");
        oldReadAlert.setPatientId(patientId);
        oldReadAlert.setRead(true);

        when(alertRepository.findByPatientId(patientId))
                .thenReturn(List.of(savedAlert, oldReadAlert));

        // when
        List<AlertDto> result = alertService.getAllNotifications(patientId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertFalse(result.get(0).isRead()); // Pierwszy pochodzi z savedAlert (nieprzeczytany)
        assertTrue(result.get(1).isRead());  // Drugi z oldReadAlert (przeczytany)

        verify(alertRepository, times(1)).findByPatientId(patientId);
    }
}