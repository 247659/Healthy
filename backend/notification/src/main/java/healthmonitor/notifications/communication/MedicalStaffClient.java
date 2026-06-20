package healthmonitor.notifications.communication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .block(); // Blokujemy, bo jesteśmy w kodzie synchronicznym RabbitMQ
    }
}
