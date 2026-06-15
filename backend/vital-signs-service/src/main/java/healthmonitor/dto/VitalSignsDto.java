package healthmonitor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VitalSignsDto {

    @NotBlank(message = "Patient ID cannot be empty")
    private String patientId;

    @NotBlank
    private String timestamp;

    @Valid
    @NotNull
    private Measurements measurements;

    @Data
    public static class Measurements {
        @Min(value = 0, message = "Heart rate cannot be negative")
        @Max(value = 300, message = "Heart rate is unrealistically high")
        private int heartRate;

        @Valid
        @NotNull
        private BloodPressure bloodPressure;

        @Min(value = 20, message = "Temperature cannot be below 20 degrees")
        @Max(value = 45, message = "Temperature cannot be above 45 degrees")
        private double temperature;

        @Min(value = 0, message = "SpO2 cannot be negative")
        @Max(value = 100, message = "SpO2 cannot be above 100%")
        private int spO2;
    }

    @Data
    public static class BloodPressure {
        @Min(0) @Max(300)
        private int systolic;

        @Min(0) @Max(200)
        private int diastolic;
    }
}
