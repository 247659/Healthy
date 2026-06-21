package healthmonitor.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.auth.exception.AuthException;
import healthmonitor.auth.exception.ErrorCode;
import healthmonitor.auth.model.dto.TokenResponseDto;
import healthmonitor.auth.publisher.RegistrationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.UserResource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.MockedStatic;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RegistrationPublisher registrationPublisher;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    @BeforeEach
    void setUp() {
        // Wstrzyknięcie wartości z application.properties przez ReflectionTestUtils
        ReflectionTestUtils.setField(keycloakUserService, "serverUrl", "http://localhost:9090");
        ReflectionTestUtils.setField(keycloakUserService, "realm", "healthmonitor-realm");
        ReflectionTestUtils.setField(keycloakUserService, "clientId", "auth-service-backend");
        ReflectionTestUtils.setField(keycloakUserService, "clientSecret", "secret");
    }

    @Test
    void loginPatient_ShouldReturnTokens_WhenCredentialsAreValidAndRoleMatches() throws Exception {
        // given
        String email = "patient@test.com";
        String password = "password";

        // Tworzenie "fałszywego" tokena JWT ze zdefiniowaną rolą (format: header.payload.signature)
        String payloadJson = "{\"realm_access\":{\"roles\":[\"PATIENT\"]}}";
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payloadJson.getBytes());
        String mockAccessToken = "header." + encodedPayload + ".signature";

        Map<String, Object> mockResponseBody = Map.of(
                "access_token", mockAccessToken,
                "refresh_token", "refresh-token"
        );

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // Prawdziwy ObjectMapper tylko po to, żeby ułatwić mockowanie sparsowanego JsonNode
        ObjectMapper realMapper = new ObjectMapper();
        JsonNode jsonNode = realMapper.readTree(payloadJson);
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);

        // when
        TokenResponseDto response = keycloakUserService.loginPatient(email, password);

        // then
        assertNotNull(response);
        assertEquals(mockAccessToken, response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        // Weryfikacja adresu URL
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForEntity(urlCaptor.capture(), any(HttpEntity.class), eq(Map.class));
        assertTrue(urlCaptor.getValue().endsWith("/realms/healthmonitor-realm/protocol/openid-connect/token"));
    }

    @Test
    void loginPatient_ShouldThrowAuthException_WhenRoleIsIncorrect() throws Exception {
        // given
        // Przekazujemy token zawierający rolę "DOCTOR", podczas gdy logujemy pacjenta
        String payloadJson = "{\"realm_access\":{\"roles\":[\"DOCTOR\"]}}";
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payloadJson.getBytes());
        String mockAccessToken = "header." + encodedPayload + ".signature";

        Map<String, Object> mockResponseBody = Map.of(
                "access_token", mockAccessToken,
                "refresh_token", "refresh-token"
        );

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        ObjectMapper realMapper = new ObjectMapper();
        JsonNode jsonNode = realMapper.readTree(payloadJson);
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);

        // when & then
        AuthException exception = assertThrows(AuthException.class,
                () -> keycloakUserService.loginPatient("patient@test.com", "password"));

        assertEquals(ErrorCode.INCORRECT_ROLE, exception.getErrorCode());
        assertEquals("Access denied, incorrect role", exception.getMessage());
    }

    @Test
    void loginPatient_ShouldThrowAuthException_WhenCredentialsAreInvalid() {
        // given
        // Symulacja błędu wywołania RestTemplate (np. Keycloak zwraca 401 Unauthorized)
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("401 Unauthorized"));

        // when & then
        AuthException exception = assertThrows(AuthException.class,
                () -> keycloakUserService.loginPatient("wrong@test.com", "badpass"));

        assertEquals(ErrorCode.INCORRECT_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void logout_ShouldCallRestTemplatePost() {
        // given
        String refreshToken = "my-refresh-token";

        // when
        keycloakUserService.logout(refreshToken);

        // then
        verify(restTemplate, times(1))
                .postForEntity(contains("/protocol/openid-connect/logout"), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void logout_ShouldThrowAuthException_OnRestTemplateError() {
        // given
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection Refused"));

        // when & then
        AuthException exception = assertThrows(AuthException.class,
                () -> keycloakUserService.logout("refresh-token"));

        assertEquals(ErrorCode.INTERVAL_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    void refreshAccessToken_ShouldReturnNewTokens() {
        // given
        Map<String, Object> mockResponseBody = Map.of(
                "access_token", "new-access",
                "refresh_token", "new-refresh"
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // when
        TokenResponseDto result = keycloakUserService.refreshAccessToken("old-refresh");

        // then
        assertNotNull(result);
        assertEquals("new-access", result.getAccessToken());
        assertEquals("new-refresh", result.getRefreshToken());
    }

    @Test
    void refreshAccessToken_ShouldThrowAuthException_WhenTokenExpired() {
        // given
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("400 Bad Request: Token Expired"));

        // when & then
        AuthException exception = assertThrows(AuthException.class,
                () -> keycloakUserService.refreshAccessToken("expired-refresh"));

        assertEquals(ErrorCode.TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void createUserInKeycloak_ShouldThrowAuthException_WhenEmailAlreadyExists() {
        // given
        String email = "existing@test.com";

        // Tworzymy głębokiego mocka (RETURNS_DEEP_STUBS pozwala łączyć wywołania np. realm().users()...)
        Keycloak keycloakMock = mock(Keycloak.class, RETURNS_DEEP_STUBS);
        when(keycloakMock.realm("healthmonitor-realm").users().searchByEmail(email, true))
                .thenReturn(List.of(new UserRepresentation())); // Zwraca niepustą listę (email istnieje)

        KeycloakBuilder builderMock = mock(KeycloakBuilder.class);
        when(builderMock.serverUrl(any())).thenReturn(builderMock);
        when(builderMock.realm(any())).thenReturn(builderMock);
        when(builderMock.grantType(any())).thenReturn(builderMock);
        when(builderMock.clientId(any())).thenReturn(builderMock);
        when(builderMock.clientSecret(any())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(keycloakMock);

        // Przechwytujemy statyczne wywołanie KeycloakBuilder.builder()
        try (MockedStatic<KeycloakBuilder> mockedBuilder = mockStatic(KeycloakBuilder.class)) {
            mockedBuilder.when(KeycloakBuilder::builder).thenReturn(builderMock);

            // when & then
            AuthException exception = assertThrows(AuthException.class,
                    () -> keycloakUserService.createUserInKeycloak(email, "pass", "John", "Doe", "PATIENT"));

            assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        }
    }

    @Test
    void createUserInKeycloak_ShouldCallRegisterAndPublishEvent_WhenUserIsNewAndResponseIs201() throws Exception {
        // given
        String email = "new@test.com";
        String role = "PATIENT";

        Keycloak keycloakMock = mock(Keycloak.class, RETURNS_DEEP_STUBS);

        // 1. Symulacja braku użytkownika w bazie Keycloak
        when(keycloakMock.realm("healthmonitor-realm").users().searchByEmail(email, true))
                .thenReturn(Collections.emptyList());

        // 2. Symulacja odpowiedzi z tworzenia użytkownika (metoda register())
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getLocation()).thenReturn(new URI("http://localhost/admin/realms/healthmonitor-realm/users/generated-uuid-123"));
        when(keycloakMock.realm("healthmonitor-realm").users().create(any(UserRepresentation.class))).thenReturn(mockResponse);

        // 3. Symulacja pobierania roli
        RoleRepresentation mockRole = new RoleRepresentation();
        mockRole.setName(role);
        when(keycloakMock.realm("healthmonitor-realm").roles().get(role).toRepresentation()).thenReturn(mockRole);

        KeycloakBuilder builderMock = mock(KeycloakBuilder.class);
        when(builderMock.serverUrl(any())).thenReturn(builderMock);
        when(builderMock.realm(any())).thenReturn(builderMock);
        when(builderMock.grantType(any())).thenReturn(builderMock);
        when(builderMock.clientId(any())).thenReturn(builderMock);
        when(builderMock.clientSecret(any())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(keycloakMock);

        try (MockedStatic<KeycloakBuilder> mockedBuilder = mockStatic(KeycloakBuilder.class)) {
            mockedBuilder.when(KeycloakBuilder::builder).thenReturn(builderMock);

            // when
            keycloakUserService.createUserInKeycloak(email, "pass", "John", "Doe", role);

            // then
            // Weryfikujemy, czy użyto metody do tworzenia użytkownika w Keycloaku
            verify(keycloakMock.realm("healthmonitor-realm").users()).create(any(UserRepresentation.class));

            // Weryfikujemy, czy dodano rolę do wygenerowanego ID użytkownika
            verify(keycloakMock.realm("healthmonitor-realm").users().get("generated-uuid-123").roles().realmLevel())
                    .add(anyList());

            // Weryfikujemy, czy opublikowano zdarzenie na RabbitMQ
            verify(registrationPublisher, times(1)).publishUserRegistration(eq(role), any());
        }
    }

    @Test
    void register_ShouldThrowAuthException_WhenKeycloakReturnsErrorStatus() {
        // given
        Keycloak keycloakMock = mock(Keycloak.class, RETURNS_DEEP_STUBS);

        when(keycloakMock.realm(anyString()).users().searchByEmail(anyString(), eq(true)))
                .thenReturn(Collections.emptyList());

        // Symulujemy awarię przy próbie zapisu w Keycloaku (np. status 400 lub 500)
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(400);

        Response.StatusType statusInfo = mock(Response.StatusType.class);
        when(statusInfo.getReasonPhrase()).thenReturn("Bad Request");
        when(mockResponse.getStatusInfo()).thenReturn(statusInfo);

        when(keycloakMock.realm(anyString()).users().create(any(UserRepresentation.class))).thenReturn(mockResponse);

        KeycloakBuilder builderMock = mock(KeycloakBuilder.class);
        when(builderMock.serverUrl(any())).thenReturn(builderMock);
        when(builderMock.realm(any())).thenReturn(builderMock);
        when(builderMock.grantType(any())).thenReturn(builderMock);
        when(builderMock.clientId(any())).thenReturn(builderMock);
        when(builderMock.clientSecret(any())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(keycloakMock);

        try (MockedStatic<KeycloakBuilder> mockedBuilder = mockStatic(KeycloakBuilder.class)) {
            mockedBuilder.when(KeycloakBuilder::builder).thenReturn(builderMock);

            // when & then
            AuthException exception = assertThrows(AuthException.class,
                    () -> keycloakUserService.createUserInKeycloak("test@test.com", "pass", "J", "D", "PATIENT"));

            assertEquals(ErrorCode.INTERVAL_SERVER_ERROR, exception.getErrorCode());

            // Nie powinno dojść do publikacji eventu w przypadku błędu
            verify(registrationPublisher, never()).publishUserRegistration(anyString(), any());
        }
    }

    @Test
    void changeUserPassword_ShouldResetPasswordSuccessfully() {
        // given
        String userId = "user-123";
        String newPassword = "new-password-123!";

        Keycloak keycloakMock = mock(Keycloak.class, RETURNS_DEEP_STUBS);

        KeycloakBuilder builderMock = mock(KeycloakBuilder.class);
        when(builderMock.serverUrl(any())).thenReturn(builderMock);
        when(builderMock.realm(any())).thenReturn(builderMock);
        when(builderMock.grantType(any())).thenReturn(builderMock);
        when(builderMock.clientId(any())).thenReturn(builderMock);
        when(builderMock.clientSecret(any())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(keycloakMock);

        try (MockedStatic<KeycloakBuilder> mockedBuilder = mockStatic(KeycloakBuilder.class)) {
            mockedBuilder.when(KeycloakBuilder::builder).thenReturn(builderMock);

            // when
            assertDoesNotThrow(() -> keycloakUserService.changeUserPassword(userId, newPassword));

            // then
            ArgumentCaptor<CredentialRepresentation> captor = ArgumentCaptor.forClass(CredentialRepresentation.class);
            verify(keycloakMock.realm("healthmonitor-realm").users().get(userId)).resetPassword(captor.capture());

            // Weryfikujemy, czy obiekt Credential reprezentujący hasło został odpowiednio zbudowany
            CredentialRepresentation capturedCredential = captor.getValue();
            assertEquals(newPassword, capturedCredential.getValue());
            assertEquals(CredentialRepresentation.PASSWORD, capturedCredential.getType());
            assertFalse(capturedCredential.isTemporary());
        }
    }

    @Test
    void changeUserPassword_ShouldThrowAuthException_WhenKeycloakFails() {
        // given
        String userId = "user-123";

        Keycloak keycloakMock = mock(Keycloak.class, RETURNS_DEEP_STUBS);

        // 1. Tworzymy jawnego mocka dla końcowego obiektu (UserResource)
        UserResource userResourceMock = mock(UserResource.class);

        // 2. Mówimy głębokiemu mockowi, żeby na samym końcu łańcucha zwrócił naszego userResourceMock
        when(keycloakMock.realm("healthmonitor-realm").users().get(userId)).thenReturn(userResourceMock);

        // 3. Teraz bezpiecznie i bez błędów przypinamy rzucenie wyjątku do JEDNEGO, płaskiego mocka
        doThrow(new RuntimeException("Keycloak connection error"))
                .when(userResourceMock).resetPassword(any());

        KeycloakBuilder builderMock = mock(KeycloakBuilder.class);
        when(builderMock.serverUrl(any())).thenReturn(builderMock);
        when(builderMock.realm(any())).thenReturn(builderMock);
        when(builderMock.grantType(any())).thenReturn(builderMock);
        when(builderMock.clientId(any())).thenReturn(builderMock);
        when(builderMock.clientSecret(any())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(keycloakMock);

        try (MockedStatic<KeycloakBuilder> mockedBuilder = mockStatic(KeycloakBuilder.class)) {
            mockedBuilder.when(KeycloakBuilder::builder).thenReturn(builderMock);

            // when & then
            AuthException exception = assertThrows(AuthException.class,
                    () -> keycloakUserService.changeUserPassword(userId, "new-password"));

            assertEquals(ErrorCode.INTERVAL_SERVER_ERROR, exception.getErrorCode());
        }
    }
}