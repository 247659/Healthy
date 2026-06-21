package healthmonitor.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import healthmonitor.auth.exception.AuthException;
import healthmonitor.auth.model.UserRegisteredEvent;
import healthmonitor.auth.model.dto.*;
import healthmonitor.auth.publisher.RegistrationPublisher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {"eureka.client.enabled=false"})
@Testcontainers
public class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistrationPublisher registrationPublisher;

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("keycloak.server-url", wireMockServer::baseUrl);
        registry.add("keycloak.realm", () -> "healthmonitor-realm");
        registry.add("keycloak.client-id", () -> "admin-cli");
        registry.add("keycloak.client-secret", () -> "secret");
    }

    @BeforeEach
    void resetWireMockAndStubAdminToken() {
        wireMockServer.resetAll();

        wireMockServer.stubFor(post(urlEqualTo("/realms/healthmonitor-realm/protocol/openid-connect/token"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .willReturn(okJson("{\"access_token\": \"fake-admin-token\", \"expires_in\": 3600, \"token_type\": \"Bearer\"}")));
    }

    @Test
    void registerPatient_ShouldRequestKeycloakAndPublishEvent_WhenDataIsValid() {
        PatientRegistrationDto dto = new PatientRegistrationDto();
        dto.setEmail("new-patient@test.com");
        dto.setPassword("StrongPass123!");
        dto.setFirstName("Jan");
        dto.setLastName("Kowalski");

        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/healthmonitor-realm/users"))
                .withQueryParam("email", equalTo("new-patient@test.com"))
                .willReturn(okJson("[]")));

        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/healthmonitor-realm/users"))
                .willReturn(created().withHeader("Location", wireMockServer.baseUrl() + "/admin/realms/healthmonitor-realm/users/generated-uuid-123")));

        wireMockServer.stubFor(get(urlEqualTo("/admin/realms/healthmonitor-realm/roles/PATIENT"))
                .willReturn(okJson("{\"name\":\"PATIENT\"}")));

        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/healthmonitor-realm/users/generated-uuid-123/role-mappings/realm"))
                .willReturn(noContent()));

        assertDoesNotThrow(() -> authService.registerPatient(dto));

        // Weryfikacja delegacji do nowego Publishera
        verify(registrationPublisher).publishUserRegistration(eq("patient"), any(UserRegisteredEvent.class));
    }

    @Test
    void registerPatient_ShouldThrowException_WhenEmailAlreadyExistsInKeycloak() {
        PatientRegistrationDto dto = new PatientRegistrationDto();
        dto.setEmail("existing@test.com");

        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/healthmonitor-realm/users"))
                .withQueryParam("email", equalTo("existing@test.com"))
                .willReturn(okJson("[{\"id\":\"existing-id\"}]")));

        assertThrows(AuthException.class, () -> authService.registerPatient(dto));
    }

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAndRoleAreValid() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("patient@test.com", "password");
        String mockAccessToken = "header.eyJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiUEFUSUVOVCJdfX0.signature";

        Map<String, String> keycloakResponse = Map.of(
                "access_token", mockAccessToken,
                "refresh_token", "refresh-123"
        );

        wireMockServer.stubFor(post(urlEqualTo("/realms/healthmonitor-realm/protocol/openid-connect/token"))
                .willReturn(okJson(objectMapper.writeValueAsString(keycloakResponse))));

        TokenResponseDto response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals(mockAccessToken, response.getAccessToken());
        assertEquals("refresh-123", response.getRefreshToken());
    }

    @Test
    void loginDoctor_ShouldReturnTokens_WhenDoctorCredentialsAreValid() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("doctor@test.com", "password");
        String mockAccessToken = "header.eyJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiRE9DVE9SIl19fQ.signature";

        Map<String, String> keycloakResponse = Map.of(
                "access_token", mockAccessToken,
                "refresh_token", "refresh-456"
        );

        wireMockServer.stubFor(post(urlEqualTo("/realms/healthmonitor-realm/protocol/openid-connect/token"))
                .willReturn(okJson(objectMapper.writeValueAsString(keycloakResponse))));

        TokenResponseDto response = authService.loginDoctor(loginRequest);

        assertNotNull(response);
        assertEquals(mockAccessToken, response.getAccessToken());
    }

    @Test
    void login_ShouldThrowException_WhenUserHasIncorrectRole() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("patient@test.com", "password");
        String mockAccessTokenWithWrongRole = "header.eyJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiRE9DVE9SIl19fQ.signature";

        Map<String, String> keycloakResponse = Map.of(
                "access_token", mockAccessTokenWithWrongRole,
                "refresh_token", "refresh-123"
        );

        wireMockServer.stubFor(post(urlEqualTo("/realms/healthmonitor-realm/protocol/openid-connect/token"))
                .willReturn(okJson(objectMapper.writeValueAsString(keycloakResponse))));

        assertThrows(AuthException.class, () -> authService.login(loginRequest));
    }

    @Test
    void logout_ShouldExecuteSuccessfully_WhenTokenIsValid() {
        LogoutRequestDto dto = new LogoutRequestDto();
        dto.setRefreshToken("valid-refresh-token");

        wireMockServer.stubFor(post(urlEqualTo("/realms/healthmonitor-realm/protocol/openid-connect/logout"))
                .willReturn(ok()));

        assertDoesNotThrow(() -> authService.logout(dto));
    }

    @Test
    void refresh_ShouldReturnNewTokens_WhenRefreshTokenIsValid() throws Exception {
        RefreshTokenRequestDto dto = new RefreshTokenRequestDto();
        dto.setRefreshToken("current-refresh-token");

        Map<String, String> keycloakResponse = Map.of(
                "access_token", "new-access",
                "refresh_token", "new-refresh"
        );

        wireMockServer.stubFor(post(urlEqualTo("/realms/healthmonitor-realm/protocol/openid-connect/token"))
                .willReturn(okJson(objectMapper.writeValueAsString(keycloakResponse))));

        TokenResponseDto response = authService.refresh(dto);

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
    }

    @Test
    void changePassword_ShouldExecuteSuccessfully_WhenUserExists() {
        String userId = "user-123";
        String newPassword = "NewPassword123!";

        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/healthmonitor-realm/users/user-123/reset-password"))
                .willReturn(noContent()));

        assertDoesNotThrow(() -> authService.changePassword(userId, newPassword));
    }
}