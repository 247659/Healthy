package healthmonitor.payload;

public record PatientClientResponse(
        String id,
        String firstName,
        String lastName
) {
    public static PatientClientResponse unfetched(String id) {
        return new PatientClientResponse(
               id,
               "Unfetched",
               "Patient"
        );
    }
}
