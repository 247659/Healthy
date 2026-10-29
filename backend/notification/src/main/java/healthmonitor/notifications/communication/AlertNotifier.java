package healthmonitor.notifications.communication;

import healthmonitor.notifications.model.AlertDto;

public interface AlertNotifier {
    void notifyDoctor(AlertDto alert, String doctorId);
}
