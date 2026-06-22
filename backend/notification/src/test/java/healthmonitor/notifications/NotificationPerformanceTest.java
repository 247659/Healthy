package healthmonitor.notifications;

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
public class NotificationPerformanceTest {

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
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Test
    public void runPerformanceTest() throws IOException {
        String baseUrl = "http://localhost:" + port + "/api/v1/notifications";

        testPlan(
                threadGroup(80, 1,
                        httpHeaders()
                                .header("Accept", "application/json"),

                        httpSampler("Get Patient Notifications", baseUrl + "/${__RandomString(4,1234567890)}")
                                .method("GET"),

                        httpSampler("Get All History", baseUrl + "/all/${__RandomString(4,1234567890)}")
                                .method("GET")
                ),
                htmlReporter("target/jmeter/reports/notifications")
        ).run();
    }
}