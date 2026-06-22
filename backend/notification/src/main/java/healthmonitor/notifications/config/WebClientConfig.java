package healthmonitor.notifications.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

    @Bean
    @Profile("prod")
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .filter(resilienceFilter());
    }

    @Bean
    @Profile("!prod")
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(resilienceFilter());
    }

    private ExchangeFilterFunction resilienceFilter() {
        return ((request, next) -> {
            boolean isIdempotent = request.method().name().equals("GET") ||
                    request.method().name().equals("PUT") ||
                    request.method().name().equals("DELETE");

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
            }
            return responseMono;
        });
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            return e.getStatusCode().is5xxServerError();
        }
        return throwable instanceof TimeoutException || throwable instanceof IOException;
    }
}
