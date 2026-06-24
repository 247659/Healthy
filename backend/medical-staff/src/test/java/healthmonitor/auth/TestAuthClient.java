package healthmonitor.auth;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class TestAuthClient {
    private final WebClient webClient;

    public TestAuthClient(String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    public String fetchToken(String email, String password) {
        Map<String, String> loginRequest = Map.of(
                "email", email,
                "password", password
        );

        return webClient.post()
                .uri("/auth/login")
                .bodyValue(loginRequest)
                .retrieve()
                .onStatus(status -> status.value() == 401 || status.value() == 404,
                        response -> Mono.empty())
                .bodyToMono(Map.class)
                .switchIfEmpty(registerAndLogin(email, password))
                .map(response -> (String) response.get("accessToken"))
                .block();
    }

    private Mono<Map> registerAndLogin(String email, String password) {
        Map<String, String> registerRequest = Map.of(
                "firstName", "Perf",
                "lastName", "Test",
                "email", email,
                "password", password
        );

        return webClient.post()
                .uri("/auth/register/patient")
                .bodyValue(registerRequest)
                .retrieve()
                .bodyToMono(Map.class);
    }
}
