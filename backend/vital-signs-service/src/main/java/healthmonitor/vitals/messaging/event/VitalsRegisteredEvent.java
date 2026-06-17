package healthmonitor.vitals.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalsRegisteredEvent {
    private String patientId;
    private Instant timestamp;
    private int heartRate;
    private int systolicBp;
    private int diastolicBp;
    private double temperature;
    private int spO2;
}