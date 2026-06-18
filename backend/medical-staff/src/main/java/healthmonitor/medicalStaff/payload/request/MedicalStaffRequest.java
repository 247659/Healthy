package healthmonitor.medicalStaff.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record MedicalStaffRequest(
        @NotBlank
        String keycloakUserId,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$")
        String phoneNumber,

        @Pattern(regexp = "^[1-9][0-9]{6}$")
        String licenseNumber,

        List<SpecializationRequest> specializations
) {
}
