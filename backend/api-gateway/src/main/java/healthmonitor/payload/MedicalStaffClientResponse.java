package healthmonitor.payload;

import java.util.Collections;
import java.util.List;

public record MedicalStaffClientResponse(
        String id,
        String firstName,
        String lastName,
        List<SpecializationClientResponse> specializations
) {
    public static MedicalStaffClientResponse unfetched(String id) {
        return new MedicalStaffClientResponse(
                id,
                "Unfetched",
                "Staff",
                Collections.emptyList()
        );
    }
}
