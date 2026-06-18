package healthmonitor.visitsservice.visits.payload.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID medicalStaffId,
        UUID patientId,
        LocalDateTime visitTime,
        Integer durationMinutes,
        String note,
        LocalDateTime createdAt
) {
}
