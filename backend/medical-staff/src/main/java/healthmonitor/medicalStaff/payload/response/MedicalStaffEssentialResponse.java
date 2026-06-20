package healthmonitor.medicalStaff.payload.response;

import java.util.List;

public record MedicalStaffEssentialResponse(
        String firstName,
        String lastName,
        List<String > specializationNames
) {
}
