package healthmonitor.notifications.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertDto {
    private String alertId;
    private String patientId;
    private Double riskScore;
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;
}