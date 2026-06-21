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
public class PatientPerformanceTest {

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
        String baseUrl = "http://localhost:" + port + "/api/v1/patients";

        testPlan(
                threadGroup(150, 1,
                        httpHeaders()
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json"),
                        httpSampler("Get All Patients", baseUrl + "/allPatients")
                                .method("GET")
                                .children(
                                        uniformRandomTimer(Duration.ofSeconds(0), Duration.ofSeconds(5))
                                ),
                        constantTimer(Duration.ofSeconds(1)),
                        httpSampler("Save New Patient", baseUrl)
                                .method("POST")
                                .body("""
                                        {
                                          "id": "${__RandomString(8,abcdef0123456789)}",
                                          "firstName": "Jan",
                                          "lastName": "Kowalski",
                                          "email": "test${__RandomString(8,abcdef0123456789)}@test.com",
                                          "pesel": "${__Random(10000000000,99999999999)}",
                                          "dateOfBirth": "1990-01-01",
                                          "phoneNumber": "123456789",
                                          "address": "Warszawa"
                                        }""")
                ),
                htmlReporter("target/jmeter/reports")
        ).run();
    }
}