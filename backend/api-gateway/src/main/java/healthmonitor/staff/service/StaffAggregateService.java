package healthmonitor.staff.service;

import healthmonitor.client.MedicalStaffClient;
import healthmonitor.client.PatientClient;
import healthmonitor.payload.PatientClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class StaffAggregateService {

    private final MedicalStaffClient medicalStaffClient;
    private final PatientClient patientClient;

    public Flux<PatientClientResponse> getAssignedPatient(String staffId) {
        return medicalStaffClient.getAssignedPatientIds(staffId)
                .flatMap(patientClient::getPatient);
    }
}
