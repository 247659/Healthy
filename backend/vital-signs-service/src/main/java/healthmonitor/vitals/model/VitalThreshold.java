package healthmonitor.vitals.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "vital_thresholds")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VitalThreshold {
    @Id
    private String patientId;
    private Integer systolicBp = 120;

    private Integer diastolicBp = 80;

    private Integer heartRate = 70;

    private Integer spO2 = 95;

    private BigDecimal temperature = BigDecimal.valueOf(36.6);
}
