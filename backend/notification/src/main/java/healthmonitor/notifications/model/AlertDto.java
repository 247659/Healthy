package healthmonitor.notifications.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AlertDto {
    private String alertId;
    private String patientId;
    private Double riskScore;
    private String severity;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;
    private boolean isRead;
}