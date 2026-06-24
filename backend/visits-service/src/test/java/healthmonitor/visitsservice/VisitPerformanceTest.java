package healthmonitor.visitsservice;

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
public class VisitPerformanceTest {

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
    }

    @Test
    public void runPerformanceTest() throws IOException {
        String baseUrl = "http://localhost:" + port + "/api/v1/visits";

        testPlan(
                threadGroup(50, 1,
                        httpHeaders()
                                .header("Content-Type", "application/json"),

                        httpSampler("Get All Visits", baseUrl)
                                .method("GET"),

                        httpSampler("Create Visit", baseUrl)
                                .method("POST")
                                .body("""
                                    {
                                      "medicalStaffId": "${__UUID}",
                                      "patientId": "patient-${__Random(1,100)}",
                                      "visitTime": "2026-07-01T10:00:00",
                                      "durationMinutes": 30,
                                      "note": "Kontrola okresowa"
                                    }""")
                ),
                htmlReporter("target/jmeter/reports/visits")
        ).run();
    }
}