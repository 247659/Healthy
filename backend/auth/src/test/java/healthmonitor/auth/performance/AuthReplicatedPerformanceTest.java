package healthmonitor.auth.performance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

public class AuthReplicatedPerformanceTest {
    private static final String GATEWAY_URL = "http://localhost:18080/api/v1/auth";

    @Test
    public void runPerformanceTest() throws IOException {
        testPlan(
                threadGroup()
                        .rampTo(50, Duration.ofSeconds(10))
                        .holdFor(Duration.ofSeconds(30))
                        .children(
                                httpHeaders()
                                        .header("Content-Type", "application/json"),

                                httpSampler("Register Patient", GATEWAY_URL + "/register/patient")
                                        .method("POST")
                                        .body(s -> {
                                            String email = UUID.randomUUID() + "@test.com";
                                            s.vars.put("myEmail", email);
                                            return "{\"firstName\":\"Perf\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Password!123\"}";
                                        }),

                                constantTimer(Duration.ofMillis(2000)),

                                httpSampler("Login patient", GATEWAY_URL + "/login")
                                        .method("POST")
                                        .body(s -> "{\"email\":\"" + s.vars.get("myEmail") + "\",\"password\":\"Password!123\"}")
                        ),
                htmlReporter("target/jmeter/reports/auth/replicated")
        ).run();
    }
}
