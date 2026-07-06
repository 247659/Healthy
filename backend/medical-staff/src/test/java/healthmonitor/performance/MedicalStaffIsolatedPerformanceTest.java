package healthmonitor.performance;

import healthmonitor.medicalStaff.model.MedicalStaff;
import healthmonitor.medicalStaff.repository.MedicalStaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class MedicalStaffIsolatedPerformanceTest {

    @Autowired
    private MedicalStaffRepository medicalStaffRepository;

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
        registry.add("management.health.rabbit.enabled", () -> "false");
        registry.add("management.health.eureka.enabled", () -> "false");
        registry.add("spring.jpa.properties.hibernate.jdbc.batch_size", () -> "50");
        registry.add("spring.jpa.properties.hibernate.order_inserts", () -> "true");
    }

    private void warmup(String baseUrl) {
        RestTemplate restTemplate = new RestTemplate();
        for (int i = 0; i < 20; i++) {
            try { restTemplate.getForObject(baseUrl, String.class); } catch (Exception ignored) {}
        }
    }

    protected void seedData(int count) {
        List<MedicalStaff> staffList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MedicalStaff ms = new MedicalStaff();
            ms.setId("ID-" + i);
            ms.setFirstName("First" + i);
            ms.setLastName("Last" + i);
            ms.setPhoneNumber("+48" + (10000000 + i));
            ms.setLicenseNumber(String.valueOf((1000000 + i)));
            staffList.add(ms);
        }
        medicalStaffRepository.saveAll(staffList);
    }

    @Test
    public void runPerformanceTest() throws IOException {
        String baseUrl = "http://localhost:" + port + "/api/v1/staff";
        seedData(10000);
        warmup(baseUrl);

        testPlan(
                threadGroup(100, 1,
                        httpHeaders()
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json"),

                        httpSampler("Get All Staff", baseUrl)
                                .method("GET"),

                        constantTimer(Duration.ofMillis(500)),

                        httpSampler("Get Essential Staff", baseUrl + "/essential")
                                .method("GET")
                ),
                htmlReporter("target/jmeter/reports/medical-staff/isolated")
        ).run();
    }
}