package healthmonitor.notifications.communication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class MedicalStaffClient {

    private final WebClient webClient;

    public MedicalStaffClient(WebClient.Builder webClientBuilder,
                              @Value("${medical-staff-service.url}") String url) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    public List<String> getDoctorIdsForPatient(String patientId) {
        return webClient.get()
                .uri("/api/v1/staff/patients/{patientId}/doctors-list", patientId)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Medical staff service is unavailable"))
                )
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .block();
    }
}
