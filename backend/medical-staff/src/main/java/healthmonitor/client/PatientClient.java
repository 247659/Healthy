package healthmonitor.client;

import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class PatientClient {
    private final WebClient webClient;

    public PatientClient(WebClient.Builder webClientBuilder,
                         @Value("${patient-service.url}") String url) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    public PatientClientResponse getPatient(String id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/patients/{id}").build(id))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new NotFoundException("Patient not found"))
                )
                .bodyToMono(PatientClientResponse.class)
                .onErrorResume(e -> {
                    if (e instanceof NotFoundException) {
                        return Mono.error(e);
                    }
                    log.error("Failed to fetch patient after retries: {}", id, e);
                    return Mono.just(PatientClientResponse.unfetched(id));
                })
                .block();
    }
}
