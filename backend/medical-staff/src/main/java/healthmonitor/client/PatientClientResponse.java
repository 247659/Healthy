package healthmonitor.client;

import java.time.LocalDate;

public record PatientClientResponse(
        String id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phoneNumber
) {
}
