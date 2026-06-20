package healthmonitor.notifications.model;

import healthmonitor.notifications.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "patient_alerts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String alertId;

    @Column(name = "patientId", nullable = false)
    private String patientId;

    @Column(name = "riskScore", nullable = false)
    private Double riskScore;

    private String severity;

    @Column(name = "message", nullable = false)
    private String message;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> details;
    private String recommendation;
    private String forecastNote;
    private String method;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "isRead", nullable = false)
    private boolean isRead = false;
}
