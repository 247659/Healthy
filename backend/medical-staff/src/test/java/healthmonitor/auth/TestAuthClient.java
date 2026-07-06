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
                .uri("/login")
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("accessToken"))
                .onErrorResume(error -> {
                    System.out.println(">>> Logowanie nie powiodło się (" + error.getMessage() + "). Trwa rejestracja...");
                    return registerAndLogin(email, password)
                            .map(res -> (String) res.get("accessToken"));
                })
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
                .uri("/register/patient")
                .bodyValue(registerRequest)
                .retrieve()
                .bodyToMono(Map.class);
    }
}
