package healthmonitor.medicalStaff.payload.response;

import java.util.List;

public record MedicalStaffResponse(
        String id,
        String firstName,
        String lastName,
        String phoneNumber,
        String licenseNumber,
        List<SpecializationResponse> specializations
) {
}
