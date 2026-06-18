package healthmonitor.payload;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record MedicalStaffClientResponse(
        UUID id,
        String firstName,
        String lastName,
        List<SpecializationClientResponse> specializations
) {
    public static MedicalStaffClientResponse unfetched(UUID id) {
        return new MedicalStaffClientResponse(
                id,
                "Unfetched",
                "Staff",
                Collections.emptyList()
        );
    }
}
