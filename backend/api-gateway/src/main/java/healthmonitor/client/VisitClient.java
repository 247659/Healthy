package healthmonitor.client;

import healthmonitor.payload.VisitClientResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
public class VisitClient {
    private final WebClient webClient;

    public VisitClient(WebClient.Builder webClientBuilder,
                       @Value("${visit-service.url}") String url) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    public Mono<VisitClientResponse> getVisit(UUID id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/visits/{id}").build(id))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found"))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Visit service is unavailable"))
                )
                .bodyToMono(VisitClientResponse.class)
                .timeout(Duration.ofSeconds(10));
    }
}
