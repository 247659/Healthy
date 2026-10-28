package healthmonitor.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@TestConfiguration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class TestSecurityConfig {

    @Bean
    SecurityWebFilterChain testSecurityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges ->
                        exchanges.anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(token ->
                                        Mono.just(
                                                new JwtAuthenticationToken(
                                                        token,
                                                        List.of(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_DOCTOR"
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ))
                .build();
    }

    @Bean
    ReactiveJwtDecoder testJwtDecoder() {
        return token -> {
            if (!"test-token".equals(token)) {
                return Mono.error(
                        new BadJwtException("Invalid test token")
                );
            }

            Jwt jwt = Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("staff1")
                    .build();

            return Mono.just(jwt);
        };
    }
}
