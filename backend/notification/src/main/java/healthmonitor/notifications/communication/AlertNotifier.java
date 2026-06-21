package healthmonitor.notifications.communication;

import healthmonitor.notifications.model.Alert;

import java.util.List;

public interface AlertNotifier {
    void notifyDoctors(Alert alert, List<String> doctorIds);
}
