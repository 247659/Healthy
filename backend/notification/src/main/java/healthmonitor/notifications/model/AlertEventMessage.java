package healthmonitor.notifications.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlertEventMessage {
    private String patientId;
    private Double riskScore;
    private String severity;
    private String message;
    private List<String> details;
    private String recommendation;
    private String forecastNote;
    private String method;
    private LocalDateTime timestamp;
}
