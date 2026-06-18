package healthmonitor.payload;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        LocalDateTime visitTime,
        Integer durationMinutes,
        String note,
        MedicalStaffClientResponse medicalStaff,
        PatientClientResponse patient
) {
    public VisitResponse(VisitClientResponse visit, MedicalStaffClientResponse medicalStaff, PatientClientResponse patient) {
        this(visit.id(), visit.visitTime(), visit.durationMinutes(), visit.note(), medicalStaff, patient);
    }
}
