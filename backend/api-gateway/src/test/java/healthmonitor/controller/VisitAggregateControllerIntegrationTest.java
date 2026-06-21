package healthmonitor.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "eureka.client.enabled=false")
@AutoConfigureWebTestClient
@ImportAutoConfiguration(exclude = {ReactiveWebSecurityAutoConfiguration.class, ReactiveOAuth2ResourceServerAutoConfiguration.class})
@ActiveProfiles("test")
class VisitAggregateControllerIntegrationTest {

    private static final WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

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
        registry.add("visit-service.url", wireMockServer::baseUrl);
        registry.add("medical-staff-service.url", wireMockServer::baseUrl);
        registry.add("patient-service.url", wireMockServer::baseUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAggregateVisitDataFromServices() {
        UUID visitId = UUID.randomUUID();

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/visits/" + visitId))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + visitId + "\", \"medicalStaffId\":\"staff1\", \"patientId\":\"pat1\"}")));

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/staff/staff1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"staff1\", \"firstName\":\"Dr.\", \"lastName\":\"House\"}")));

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/patients/pat1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"pat1\", \"firstName\":\"John\", \"lastName\":\"Doe\"}")));

        webTestClient.get()
                .uri("/api/v1/gateway/dashboard/visits/{id}", visitId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(visitId.toString())
                .jsonPath("$.medicalStaff.firstName").isEqualTo("Dr.")
                .jsonPath("$.medicalStaff.lastName").isEqualTo("House")
                .jsonPath("$.patient.firstName").isEqualTo("John")
                .jsonPath("$.patient.lastName").isEqualTo("Doe");
    }
}