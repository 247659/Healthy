package healthmonitor.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import healthmonitor.payload.PatientClientResponse;
import healthmonitor.staff.service.StaffAggregateService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(properties = {
        "medical-staff-service.url=http://placeholder",
        "patient-service.url=http://placeholder",
        "eureka.client.enabled=false"
})
class StaffAggregateServiceIntegrationTest {

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void start() {
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stop() { wireMockServer.stop(); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("medical-staff-service.url", wireMockServer::baseUrl);
        registry.add("patient-service.url", wireMockServer::baseUrl);
    }

    @Autowired
    private StaffAggregateService service;

    @Test
    void shouldReturnAssignedPatients() {
        String staffId = "staff1";

        stubFor(get(urlEqualTo("/api/v1/staff/" + staffId + "/patients"))
                .willReturn(okJson("[\"pat1\", \"pat2\"]")));

        stubFor(get(urlEqualTo("/api/v1/patients/pat1"))
                .willReturn(okJson("{\"id\":\"pat1\", \"firstName\":\"John\"}")));

        stubFor(get(urlEqualTo("/api/v1/patients/pat2"))
                .willReturn(okJson("{\"id\":\"pat2\", \"firstName\":\"Jane\"}")));

        Flux<PatientClientResponse> result = service.getAssignedPatients(staffId);

        StepVerifier.create(result.collectList())
                .expectNextMatches(list -> list.size() == 2 &&
                        list.stream().anyMatch(p -> p.id().equals("pat1") && "John".equals(p.firstName())) &&
                        list.stream().anyMatch(p -> p.id().equals("pat2") && "Jane".equals(p.firstName())))
                .verifyComplete();
    }

    @Test
    void shouldReturnUnassignedPatients() {
        String staffId = "staff1";

        stubFor(get(urlEqualTo("/api/v1/staff/" + staffId + "/patients"))
                .willReturn(okJson("[\"pat1\"]")));

        stubFor(get(urlEqualTo("/api/v1/patients/allPatients"))
                .willReturn(okJson("[{\"id\":\"pat1\", \"firstName\":\"John\"}, {\"id\":\"pat2\", \"firstName\":\"Jane\"}]")));

        Flux<PatientClientResponse> result = service.getUnassignedPatients(staffId);

        StepVerifier.create(result)
                .expectNextMatches(p -> "pat2".equals(p.id()) && "Jane".equals(p.firstName()))
                .verifyComplete();
    }
}