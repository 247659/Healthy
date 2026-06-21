package healthmonitor.client;

import healthmonitor.payload.MedicalStaffClientResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class MedicalStaffClient {
    private final WebClient webClient;

    public MedicalStaffClient(WebClient.Builder webClientBuilder,
                              @Value("${medical-staff-service.url}") String url) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    public Mono<MedicalStaffClientResponse> getMedicalStaff(String id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/staff/{id}").build(id))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found"))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Visit service is unavailable"))
                )
                .bodyToMono(MedicalStaffClientResponse.class)
                .timeout(Duration.ofSeconds(10));
    }

    public Flux<String> getAssignedPatientIds(String id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/staff/{id}/patients").build(id))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .flatMapMany(Flux::fromIterable)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> Flux.empty());
    }
}
