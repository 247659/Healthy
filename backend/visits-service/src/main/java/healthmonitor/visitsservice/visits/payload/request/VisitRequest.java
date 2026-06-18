package healthmonitor.visitsservice.visits.payload.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitRequest(
        @NotNull
        UUID medicalStaffId,

        @NotNull
        String patientId,

        @NotNull
        LocalDateTime visitTime,

        @NotNull
        Integer durationMinutes,

        String note
) {
}
