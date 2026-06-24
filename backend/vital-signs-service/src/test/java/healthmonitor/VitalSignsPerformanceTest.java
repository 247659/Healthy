package healthmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class VitalSignsPerformanceTest {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static InfluxDBContainer<?> influxDB = new InfluxDBContainer<>(DockerImageName.parse("influxdb:2.7"))
            .withOrganization("test_org")
            .withBucket("test_bucket")
            .withAdminToken("my-super-secret-token");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("influxdb.url", influxDB::getUrl);
        registry.add("influxdb.bucket", () -> "test_bucket");
        registry.add("influxdb.org", () -> "test_org");
        registry.add("influxdb.token", () -> "my-super-secret-token");

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Test
    public void runPerformanceTest() throws IOException {
        String baseUrl = "http://localhost:" + port + "/api/v1/vital-signs";

        testPlan(
                threadGroup(60, 1,
                        httpHeaders().header("Content-Type", "application/json"),

                        httpSampler("Get History", baseUrl + "/patient/patient-123")
                                .method("GET")
                ),
                htmlReporter("target/jmeter/reports/vitals")
        ).run();
    }
}