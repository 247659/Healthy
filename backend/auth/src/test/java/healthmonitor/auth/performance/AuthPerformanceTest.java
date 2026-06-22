package healthmonitor.auth.performance;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AuthPerformanceTest {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.0")
            .withRealmImportFile("realm-test.json")
            .withReuse(false);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("keycloak.auth-server-url", keycloak::getAuthServerUrl);
        registry.add("keycloak.realm", () -> "test-realm");

        registry.add("spring.rabbitmq.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Test
    public void runPerformanceTest() throws IOException {
        String baseUrl = "http://localhost:" + port + "/api/v1/auth";
        System.out.println("KEYCLOAK URL: " + keycloak.getAuthServerUrl());

        testPlan(
                threadGroup(40, 1,
                        httpHeaders().header("Content-Type", "application/json"),

                        httpSampler("Register & Login", baseUrl + "/register/patient")
                                .method("POST")
                                .body("""
                                        {
                                          "email": "user${__Random(1,999999)}@test.com",
                                          "firstName": "Jan",
                                          "lastName": "Kowalski",
                                          "password": "Password123!"
                                        }""")
                ),
                htmlReporter("target/jmeter/reports/auth")
        ).run();
    }
}