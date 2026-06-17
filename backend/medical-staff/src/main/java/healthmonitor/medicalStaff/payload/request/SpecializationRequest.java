package healthmonitor.medicalStaff.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record SpecializationRequest(
        @NotBlank
        String name,

        @NotNull
        LocalDate obtainedDate,

        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{6}$")
        String certificateNumber
) {
}
