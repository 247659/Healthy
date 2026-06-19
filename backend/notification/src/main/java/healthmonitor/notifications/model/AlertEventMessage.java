package healthmonitor.notifications.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertEventMessage {
    private String patientId;
    private Double riskScore;
    private String message;
    private LocalDateTime timestamp;
}
