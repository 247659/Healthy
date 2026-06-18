package healthmonitor.payload;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitClientResponse(
        UUID id,
        UUID medicalStaffId,
        String patientId,
        LocalDateTime visitTime,
        Integer durationMinutes,
        String note
) {
}
