package healthmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class MedicalStaffPerformanceTest {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("management.metrics.export.otlp.enabled", () -> "false");
    }

    @Test
    public void runPerformanceTest() throws IOException {
        String baseUrl = "http://localhost:" + port + "/api/v1/staff";

        testPlan(
                threadGroup(100, 1,
                        httpHeaders()
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json"),

                        httpSampler("Get All Staff", baseUrl)
                                .method("GET"),

                        constantTimer(Duration.ofMillis(500)),

                        httpSampler("Save New Staff", baseUrl)
                                .method("POST")
                                .body("""
                                        {
                                          "firstName": "Anna",
                                          "lastName": "Nowak",
                                          "specialization": "Kardiolog",
                                          "email": "doctor${__RandomString(6,abcdef0123456789)}@test.com",
                                          "phoneNumber": "987654321"
                                        }"""),

                        httpSampler("Get Essential Staff", baseUrl + "/essential")
                                .method("GET")
                ),
                htmlReporter("target/jmeter/reports/medical-staff")
        ).run();
    }
}