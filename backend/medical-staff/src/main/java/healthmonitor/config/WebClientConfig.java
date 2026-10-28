package healthmonitor.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class WebClientConfig {

    private static final String CLIENT_REGISTRATION_ID = "keycloak-internal";

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    @Profile("prod")
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder(OAuth2AuthorizedClientManager authorizedClientManager) {
        return WebClient.builder()
                .apply(oauth2Filter(authorizedClientManager).oauth2Configuration())
                .filter(resilienceFilter());
    }

    @Bean
    @Profile("!prod")
    public WebClient.Builder webClientBuilder(OAuth2AuthorizedClientManager authorizedClientManager) {
        return WebClient.builder()
                .apply(oauth2Filter(authorizedClientManager).oauth2Configuration())
                .filter(resilienceFilter());
    }

    private ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Client.setDefaultClientRegistrationId(CLIENT_REGISTRATION_ID);
        return oauth2Client;
    }

    private ExchangeFilterFunction resilienceFilter() {
        return (request, next) -> {
            HttpMethod method = request.method();
            boolean isIdempotent = method.equals(HttpMethod.GET) ||
                    method.equals(HttpMethod.PUT) ||
                    method.equals(HttpMethod.DELETE);

            Mono<ClientResponse> responseMono = next.exchange(request)
                    .flatMap(response -> {
                        if (response.statusCode().is5xxServerError()) {
                            return response.createException().flatMap(Mono::error);
                        }
                        return Mono.just(response);
                    })
                    .timeout(Duration.ofMillis(1500));

            if (isIdempotent) {
                return responseMono.retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                        .filter(this::shouldRetry));
            } else {
                return responseMono;
            }
        };
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            return e.getStatusCode().is5xxServerError();
        }
        return throwable instanceof TimeoutException || throwable instanceof IOException;
    }
}