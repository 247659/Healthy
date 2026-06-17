package healthmonitor.client;

import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class PatientClient {
    private final WebClient webClient;
    private final PatientMapper patientMapper;

    public PatientClient(@Value("${patient-service.url}") String url, PatientMapper patientMapper) {
        this.webClient = WebClient.builder().baseUrl(url).build();
        this.patientMapper = patientMapper;
    }

    public PatientResponse getPatient(String id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/patients/{id}").build(id))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new NotFoundException("Patient not found"))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new IllegalStateException("Patient service is unavailable"))
                )
                .bodyToMono(PatientClientResponse.class)
                .map(patientMapper::toResponse)
                .block();
    }

    public List<PatientResponse> getPatients(Set<String> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return Collections.emptyList();
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/patients/allPatients").build())
                .retrieve()
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new IllegalStateException("Patient service is unavailable"))
                )
                .bodyToFlux(PatientClientResponse.class)
                .filter(patient -> patientIds.contains(patient.id()))
                .map(patientMapper::toResponse)
                .collectList()
                .block();
    }
}
