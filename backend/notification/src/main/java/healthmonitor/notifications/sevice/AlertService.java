package healthmonitor.notifications.sevice;

import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AlertService {
    void processAlert(AlertEventMessage alertDto);

    List<AlertDto> getNotificationsForPatient(String patientId, String userId);

    Page<AlertDto> getAllNotifications(String patientId, String userId, int page, int size);

    void markAsRead(List<String> ids, String userId);
}
