package healthmonitor.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import healthmonitor.payload.VisitResponse;
import healthmonitor.visit.service.VisitAggregateService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(properties = {
        "visit-service.url=http://placeholder",
        "medical-staff-service.url=http://placeholder",
        "patient-service.url=http://placeholder",
        "eureka.client.enabled=false"
})
class VisitAggregateServiceIntegrationTest {

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void start() {
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("visit-service.url", wireMockServer::baseUrl);
        registry.add("medical-staff-service.url", wireMockServer::baseUrl);
        registry.add("patient-service.url", wireMockServer::baseUrl);
    }

    @Autowired
    private VisitAggregateService service;

    @Test
    void shouldAggregateDataCorrectlyFromRealHttpCalls() {
        UUID visitId = UUID.nameUUIDFromBytes("test-visit-id".getBytes());
        String visitIdString = visitId.toString();

        stubFor(get(urlEqualTo("/api/v1/visits/" + visitIdString))
                .willReturn(okJson("{\"id\":\"" + visitIdString + "\", \"medicalStaffId\":\"m1\", \"patientId\":\"p1\"}")));

        stubFor(get(urlEqualTo("/api/v1/staff/m1"))
                .willReturn(okJson("{\"id\":\"m1\", \"firstName\":\"Dr.\", \"lastName\":\"House\"}")));

        stubFor(get(urlEqualTo("/api/v1/patients/p1"))
                .willReturn(okJson("{\"id\":\"p1\", \"firstName\":\"John\", \"lastName\":\"Doe\"}")));

        Mono<VisitResponse> result = service.getVisitResponse(visitId);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.medicalStaff().lastName().equals("House"))
                .verifyComplete();
    }
}
