package healthmonitor.payload;

import java.time.LocalDate;

public record PatientClientResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String pesel,
        LocalDate dateOfBirth,
        String phoneNumber,
        String address
) {
    public static PatientClientResponse unfetched(String id) {
        return new PatientClientResponse(
               id,
               "Unfetched",
               "Patient",
                "email@example.com",
                "PESEL",
                LocalDate.now(),
                "phone number",
                "address"
        );
    }
}
