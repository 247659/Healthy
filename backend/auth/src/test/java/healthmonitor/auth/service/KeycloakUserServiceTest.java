package healthmonitor.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.auth.client.KeycloakClient;
import healthmonitor.auth.exception.AuthException;
import healthmonitor.auth.exception.ErrorCode;
import healthmonitor.auth.model.dto.TokenResponseDto;
import healthmonitor.auth.publisher.RegistrationPublisher;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

    @Mock private KeycloakClient keycloakClient;
    @Mock private Keycloak keycloakAuthClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private RegistrationPublisher registrationPublisher;

    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    private final String REALM = "healthmonitor-realm";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakUserService, "realm", REALM);
        // Konfiguracja podstawowego łańcucha dla operacji na użytkownikach
        lenient().when(keycloakAuthClient.realm(REALM)).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
    void loginPatient_Success() throws Exception {
        String token = "header." + Base64.getUrlEncoder().encodeToString("{\"realm_access\":{\"roles\":[\"PATIENT\"]}}".getBytes()) + ".sig";
        when(keycloakClient.fetchToken(anyString(), anyString())).thenReturn(new TokenResponseDto(token, "ref"));
        when(objectMapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree("{\"realm_access\":{\"roles\":[\"PATIENT\"]}}"));

        assertNotNull(keycloakUserService.loginPatient("p@t.com", "pass"));
    }

    @Test
    void loginPatient_IncorrectRole() throws Exception {
        String token = "header." + Base64.getUrlEncoder().encodeToString("{\"realm_access\":{\"roles\":[\"DOCTOR\"]}}".getBytes()) + ".sig";
        when(keycloakClient.fetchToken(anyString(), anyString())).thenReturn(new TokenResponseDto(token, "ref"));
        when(objectMapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree("{\"realm_access\":{\"roles\":[\"DOCTOR\"]}}"));

        assertThrows(AuthException.class, () -> keycloakUserService.loginPatient("p@t.com", "pass"));
    }

    @Test
    void loginPatient_InvalidCredentials() {
        when(keycloakClient.fetchToken(anyString(), anyString())).thenThrow(new AuthException(ErrorCode.INCORRECT_CREDENTIALS));
        assertThrows(AuthException.class, () -> keycloakUserService.loginPatient("p@t.com", "pass"));
    }

    @Test
    void logout_Success() {
        assertDoesNotThrow(() -> keycloakUserService.logout("ref"));
        verify(keycloakClient).logout("ref");
    }

    @Test
    void logout_Error() {
        doThrow(new AuthException(ErrorCode.INTERVAL_SERVER_ERROR)).when(keycloakClient).logout(anyString());
        assertThrows(AuthException.class, () -> keycloakUserService.logout("ref"));
    }

    @Test
    void refresh_Success() {
        when(keycloakClient.refreshToken("ref")).thenReturn(new TokenResponseDto("acc", "new-ref"));
        assertNotNull(keycloakUserService.refreshAccessToken("ref"));
    }

    @Test
    void refresh_Error() {
        when(keycloakClient.refreshToken(anyString())).thenThrow(new AuthException(ErrorCode.TOKEN_EXPIRED));
        assertThrows(AuthException.class, () -> keycloakUserService.refreshAccessToken("exp"));
    }

    @Test
    void register_EmailExists() {
        when(usersResource.searchByEmail("e@e.com", true)).thenReturn(List.of(new UserRepresentation()));
        assertThrows(AuthException.class, () -> keycloakUserService.createUserInKeycloak("e@e.com", "p", "f", "l", "PATIENT"));
    }

    @Test
    void register_Success() throws Exception {
        Keycloak keycloakMock = mock(Keycloak.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(keycloakUserService, "keycloakAuthClient", keycloakMock);

        String email = "n@n.com";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(new URI("http://loc/users/123"));

        when(keycloakMock.realm(REALM).users().searchByEmail(email, true))
                .thenReturn(Collections.emptyList());

        when(keycloakMock.realm(REALM).users().create(any())).thenReturn(response);

        when(keycloakMock.realm(REALM).roles().get("PATIENT").toRepresentation())
                .thenReturn(new RoleRepresentation());

        keycloakUserService.createUserInKeycloak(email, "p", "f", "l", "PATIENT");

        verify(registrationPublisher).publishUserRegistration(eq("PATIENT"), any());
    }
    @Test
    void register_KeycloakError() {
        when(usersResource.searchByEmail(anyString(), eq(true))).thenReturn(Collections.emptyList());
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(usersResource.create(any())).thenReturn(response);

        assertThrows(AuthException.class, () -> keycloakUserService.createUserInKeycloak("e@e.com", "p", "f", "l", "PATIENT"));
    }

    @Test
    void changePassword_Success() {
        when(usersResource.get("123")).thenReturn(userResource);
        keycloakUserService.changeUserPassword("123", "newPass");
        verify(userResource).resetPassword(any());
    }

    @Test
    void changePassword_Error() {
        when(usersResource.get("123")).thenReturn(userResource);
        doThrow(new RuntimeException()).when(userResource).resetPassword(any());
        assertThrows(AuthException.class, () -> keycloakUserService.changeUserPassword("123", "p"));
    }
}