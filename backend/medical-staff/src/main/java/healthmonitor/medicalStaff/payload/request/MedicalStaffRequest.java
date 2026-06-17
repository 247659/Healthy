package healthmonitor.medicalStaff.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record MedicalStaffRequest(
        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$")
        String phoneNumber,

        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{6}$")
        String licenseNumber,

        @NotNull
        List<SpecializationRequest> specializations
) {
}
