package healthmonitor.client;

import healthmonitor.payload.VisitClientResponse;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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
                        response -> Mono.error(new NotFoundException("Visit not found"))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new IllegalStateException("Visit service is unavailable"))
                )
                .bodyToMono(VisitClientResponse.class)
                .timeout(Duration.ofSeconds(10));
    }
}
