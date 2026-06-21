package healthmonitor.notifications;

import healthmonitor.notifications.communication.WebSocketAlertNotifier;
import healthmonitor.notifications.model.Alert;
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
        List<String> doctorIds = List.of("doc-1", "doc-2", "doc-3");

        webSocketAlertNotifier.notifyDoctors(alert, doctorIds);

        verify(messagingTemplate, times(1)).convertAndSendToUser("doc-1", "/queue/alerts", alert);
        verify(messagingTemplate, times(1)).convertAndSendToUser("doc-2", "/queue/alerts", alert);
        verify(messagingTemplate, times(1)).convertAndSendToUser("doc-3", "/queue/alerts", alert);

        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void shouldNotSendAnything_WhenDoctorListIsEmpty() {
        Alert alert = Alert.builder().alertId(UUID.randomUUID().toString()).message("Test").build();
        List<String> emptyDoctorIds = List.of();

        webSocketAlertNotifier.notifyDoctors(alert, emptyDoctorIds);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}