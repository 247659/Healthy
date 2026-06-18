package healthmonitor.medicalStaff.messaging.event;

public record MedicalStaffRegisterEvent(
        String keycloakUserId,
        String firstName,
        String lastName
) {
}
