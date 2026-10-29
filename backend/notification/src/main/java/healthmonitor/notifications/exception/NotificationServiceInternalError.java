package healthmonitor.notifications.exception;

public class NotificationServiceInternalError extends RuntimeException {
    public NotificationServiceInternalError(String message) {
        super(message);
    }
}
