package healthmonitor.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SpringSecurity {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable()) // Wyłączamy CSRF dla API typu REST
                .authorizeExchange(exchanges -> exchanges
                        // Jeśli chcesz wypuścić jakiś adres bez logowania, np. testowy
                        // .pathMatchers("/api/v1/patients/test/hello").permitAll()

                        // Wszystkie inne żądania wymagają bycia zalogowanym
                        .anyExchange().authenticated()
                )
                // Mówimy Gatewayowi: "Sprawdzaj tokeny JWT w nagłówku Authorization"
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(org.springframework.security.config.Customizer.withDefaults()));

        return http.build();
    }
}
