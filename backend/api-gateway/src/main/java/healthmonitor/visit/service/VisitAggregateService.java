package healthmonitor.visit.service;

import healthmonitor.client.MedicalStaffClient;
import healthmonitor.client.PatientClient;
import healthmonitor.client.VisitClient;
import healthmonitor.payload.MedicalStaffClientResponse;
import healthmonitor.payload.PatientClientResponse;
import healthmonitor.payload.VisitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitAggregateService {

    private final VisitClient visitClient;
    private final MedicalStaffClient medicalStaffClient;
    private final PatientClient patientClient;

    public Mono<VisitResponse> getVisitResponse(UUID visitId) {
        return visitClient.getVisit(visitId)
                .flatMap(visit ->
                        Mono.zip(
                                fetchMedicalStaff(visit.medicalStaffId()),
                                fetchPatient(visit.patientId())
                        ).map(tuple -> new VisitResponse(
                                        visit, tuple.getT1(), tuple.getT2()
                                )
                        )
                );
    }

    private Mono<MedicalStaffClientResponse> fetchMedicalStaff(String id) {
        return medicalStaffClient.getMedicalStaff(id)
                .onErrorResume(ex -> Mono.just(
                        MedicalStaffClientResponse.unfetched(id)
                ));
    }

    private Mono<PatientClientResponse> fetchPatient(String id) {
        return patientClient.getPatient(id)
                .onErrorResume(ex -> Mono.just(
                        PatientClientResponse.unfetched(id)
                ));
    }
}
