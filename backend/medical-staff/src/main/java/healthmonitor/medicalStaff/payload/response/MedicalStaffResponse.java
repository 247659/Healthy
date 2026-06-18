package healthmonitor.medicalStaff.payload.response;

import java.util.List;
import java.util.UUID;

public record MedicalStaffResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String licenseNumber,
        List<SpecializationResponse> specializations
) {
}
