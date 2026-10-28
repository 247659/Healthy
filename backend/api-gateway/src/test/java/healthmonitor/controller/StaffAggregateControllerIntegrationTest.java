package healthmonitor.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import healthmonitor.config.TestSecurityConfig;
import healthmonitor.payload.PatientClientResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false"
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class StaffAggregateControllerIntegrationTest {

    private static final WireMockServer wireMockServer =
            new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void start() {
        wireMockServer.start();
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("medical-staff-service.url", wireMockServer::baseUrl);
        registry.add("patient-service.url", wireMockServer::baseUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnAssignedPatients() {
        String staffId = "staff1";

        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/staff/" + staffId + "/patients"))
                        .willReturn(okJson("[\"pat1\", \"pat2\"]"))
        );

        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/patients/pat1"))
                        .willReturn(okJson(
                                "{\"id\":\"pat1\",\"firstName\":\"John\"}"
                        ))
        );

        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/patients/pat2"))
                        .willReturn(okJson(
                                "{\"id\":\"pat2\",\"firstName\":\"Jane\"}"
                        ))
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/gateway/dashboard/staff/{id}/patients/assigned",
                        staffId
                )
                .headers(headers ->
                        headers.setBearerAuth("test-token"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(PatientClientResponse.class)
                .hasSize(2)
                .value(list -> {
                    assertThat(list)
                            .extracting(PatientClientResponse::firstName)
                            .containsExactlyInAnyOrder("John", "Jane");

                    assertThat(list)
                            .extracting(PatientClientResponse::id)
                            .containsExactlyInAnyOrder("pat1", "pat2");
                });
    }
}