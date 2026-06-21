package healthmonitor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class AuthClient {
    private final WebClient webClient;

    public AuthClient(WebClient.Builder webClientBuilder,
                         @Value("${auth-service.url}") String url) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }

    public void updatePassword(String keycloakUserId, String newPassword) {
        Map<String, String> body = Map.of("password", newPassword);

        webClient.put()
                .uri("/api/v1/auth/users/{id}/password", keycloakUserId)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Keycloak odrzucił zmianę hasła dla usera: {}. Status: {}", keycloakUserId, response.statusCode());
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Błąd autoryzacji"));
                })
                .toBodilessEntity()
                .doOnError(error -> log.error("Błąd połączenia z Auth Service dla usera: {}", keycloakUserId))
                .block();

        log.info("Zlecono zmianę hasła w auth-service dla ID: {}", keycloakUserId);
    }
}
