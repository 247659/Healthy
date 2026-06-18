package healthmonitor.medicalStaff.payload.response;

import java.time.LocalDate;

public record SpecializationResponse(
        String name,
        LocalDate obtainedDate,
        String certificateNumber
) {
}
