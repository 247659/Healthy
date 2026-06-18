package healthmonitor.notifications.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertDto {
    private String patientId;
    private Double riskScore;
    private String message;
    private LocalDateTime timestamp;
}