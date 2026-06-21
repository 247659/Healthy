package healthmonitor.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;


@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldDenyAccessWhenNoTokenProvided() {
        webTestClient.get()
                .uri("/api/v1/gateway/dashboard/staff/staff1/patients/assigned")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
