package healthmonitor.client;

import healthmonitor.payload.PatientClientResponse;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class PatientClient {
    private final WebClient webClient;

    public PatientClient(WebClient.Builder webClientBuilder,
                         @Value("${patient-service.url}") String url) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    public Mono<PatientClientResponse> getPatient(String id) {
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
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> Mono.just(PatientClientResponse.unfetched(id)));
    }
}
