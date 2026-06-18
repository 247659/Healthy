package healthmonitor.client;

import healthmonitor.payload.MedicalStaffClientResponse;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class MedicalStaffClient {
    private final WebClient webClient;

    public MedicalStaffClient(WebClient.Builder webClientBuilder,
                              @Value("${medical-staff-service.url}") String url) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    public Mono<MedicalStaffClientResponse> getMedicalStaff(UUID id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/staff/{id}").build(id))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new NotFoundException("Medical staff not found"))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new IllegalStateException("Medical staff service is unavailable"))
                )
                .bodyToMono(MedicalStaffClientResponse.class)
                .timeout(Duration.ofSeconds(10));
    }

    public Flux<String> getAssignedPatientIds(UUID id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/staff/{id}/patients").build(id))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .flatMapMany(Flux::fromIterable)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> Flux.empty());
    }
}
