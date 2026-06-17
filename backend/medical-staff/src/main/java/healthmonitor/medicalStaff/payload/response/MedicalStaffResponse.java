package healthmonitor.medicalStaff.payload.response;

import healthmonitor.medicalStaff.payload.request.SpecializationRequest;

import java.util.List;
import java.util.UUID;

public record MedicalStaffResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String licenseNumber,
        List<SpecializationRequest> specializations
) {
}
