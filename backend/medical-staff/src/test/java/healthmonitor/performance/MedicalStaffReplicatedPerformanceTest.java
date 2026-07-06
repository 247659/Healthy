package healthmonitor.performance;

import healthmonitor.auth.TestAuthClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

public class MedicalStaffReplicatedPerformanceTest {
    private static final String GATEWAY_URL = "http://localhost:18080/api/v1/staff";
    private static final TestAuthClient authClient = new TestAuthClient("http://localhost:18080/api/v1/auth");

    @Test
    public void runPerformanceTest() throws IOException {
        String token = authClient.fetchToken("patient@test.com", "Password123!");

        testPlan(
                threadGroup()
                        .rampTo(50, Duration.ofSeconds(10))
                        .holdFor(Duration.ofSeconds(30))
                        .children(
                                httpHeaders()
                                        .header("Authorization", "Bearer " + token)
                                        .header("Content-Type", "application/json"),

                                httpSampler("Get All Staff", GATEWAY_URL)
                                        .method("GET"),

                                constantTimer(Duration.ofMillis(2000)),

                                httpSampler("Get Essential Staff", GATEWAY_URL + "/essential")
                                        .method("GET")
                        ),
                htmlReporter("target/jmeter/reports/medical-staff/replicated")
        ).run();
    }
}
