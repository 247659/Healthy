package healthmonitor.client;

import java.time.LocalDate;

public record PatientResponse(
        String id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phoneNumber
) {
}
