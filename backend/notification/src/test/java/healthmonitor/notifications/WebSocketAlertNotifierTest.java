package healthmonitor.notifications;

import healthmonitor.notifications.communication.WebSocketAlertNotifier;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAlertNotifierTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketAlertNotifier webSocketAlertNotifier;

    @Test
    void shouldSendNotificationToAllDoctorsInList() {
        Alert alert = Alert.builder().alertId(UUID.randomUUID().toString()).message("Test").build();
        String doctorId = "doc-1";

        webSocketAlertNotifier.notifyDoctor(mapToDto(alert), doctorId);

        verify(messagingTemplate, times(1)).convertAndSendToUser("doc-1", "/queue/alerts", mapToDto(alert));

        verifyNoMoreInteractions(messagingTemplate);
    }

    private AlertDto mapToDto(Alert alert) {
        return AlertDto.builder()
                .alertId(alert.getAlertId())
                .patientId(alert.getPatientId())
                .riskScore(alert.getRiskScore())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .details(alert.getDetails())
                .timestamp(alert.getTimestamp())
                .isRead(alert.isRead())
                .build();
    }
}