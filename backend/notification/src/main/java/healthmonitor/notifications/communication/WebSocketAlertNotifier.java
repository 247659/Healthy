package healthmonitor.notifications.communication;

import healthmonitor.notifications.model.AlertDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAlertNotifier implements AlertNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyDoctor(AlertDto alert, String doctorId) {
        messagingTemplate.convertAndSendToUser(
                doctorId,
                "/queue/alerts",
                alert
        );
        log.debug("WebSocket notification sent to doctor: {}", doctorId);
    }
}
