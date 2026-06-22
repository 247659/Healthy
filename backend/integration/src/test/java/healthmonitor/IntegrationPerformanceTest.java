package healthmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class IntegrationPerformanceTest {

    @LocalServerPort
    private int port;

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

        registry.add("spring.rabbitmq.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration");
    }

    @Test
    public void runPerformanceTest() throws IOException {
        String baseUrl = "http://localhost:" + port + "/api/v1/integration";

        testPlan(
                threadGroup(50, 1,
                        httpHeaders().header("Content-Type", "application/json"),

                        httpSampler("Receive Single Vital", baseUrl)
                                .method("POST")
                                .body("""
                                        {
                                          "patientId": "p-${__Random(1,1000)}",
                                          "timestamp": "2026-06-22T01:00:00Z",
                                          "measurements": {
                                            "heartRate": 75,
                                            "bloodPressure": { "systolic": 120, "diastolic": 80 },
                                            "temperature": 36.6,
                                            "spO2": 98
                                          }
                                        }""")
                ),
                htmlReporter("target/jmeter/reports/integration")
        ).run();
    }
}