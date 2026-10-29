package healthmonitor.notifications.sevice;

import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;

import java.util.List;

public interface AlertService {
    void processAlert(AlertEventMessage alertDto);

    List<AlertDto> getNotificationsForPatient(String patientId, String userId);

    List<AlertDto> getAllNotifications(String patientId, String userId);

    void markAsRead(List<String> ids, String userId);
}
