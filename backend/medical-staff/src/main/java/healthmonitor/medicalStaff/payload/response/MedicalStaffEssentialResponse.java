package healthmonitor.medicalStaff.payload.response;

import java.util.List;

public record MedicalStaffEssentialResponse(
        String id,
        String firstName,
        String lastName,
        List<String > specializationNames
) {
}
