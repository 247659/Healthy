package healthmonitor.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.auth.exception.AuthException;
import healthmonitor.auth.exception.ErrorCode;
import healthmonitor.auth.model.UserRegisteredEvent;
import healthmonitor.auth.model.dto.TokenResponseDto;
import healthmonitor.auth.publisher.RegistrationPublisher;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserService {

    @Value("${keycloak.server-url}")
    private String serverUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RegistrationPublisher registrationPublisher;

    private final String PUBLIC_CLIENT_ID = "health-api";

    private Keycloak getKeycloakClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType("client_credentials")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    public void createUserInKeycloak(String email, String password, String firstName, String lastName, String role) {
        Keycloak keycloakClient = getKeycloakClient();
        List<UserRepresentation> existingUsers = keycloakClient.realm(realm)
                .users().searchByEmail(email, true);

        if (!existingUsers.isEmpty()) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(Collections.singletonList(credential));
        register(user, role);
    }

    public TokenResponseDto loginPatient(String email, String password) {
        return login(email, password, "PATIENT");
    }

    public TokenResponseDto loginDoctor(String email, String password) {
        return login(email, password, "DOCTOR");
    }

    private TokenResponseDto login(String email, String password, String requiredRole) {
        String tokenEndpoint = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", PUBLIC_CLIENT_ID);
        map.add("username", email);
        map.add("password", password);
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            Map<String, Object> body = response.getBody();

            String accessToken = (String) body.get("access_token");
            String refreshToken = (String) body.get("refresh_token");

            validateRole(accessToken, requiredRole);

            return new TokenResponseDto(accessToken, refreshToken);

        } catch (AuthException e) {
            throw e;
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Brak dostępu")) {
                throw e;
            }
            throw new AuthException(ErrorCode.INCORRECT_CREDENTIALS);
        } catch (Exception e) {
            throw new AuthException(ErrorCode.INCORRECT_CREDENTIALS);
        }
    }

    private void validateRole(String accessToken, String requiredRole) throws Exception {

        String[] chunks = accessToken.split("\\.");
        if (chunks.length <= 1) {
            throw new AuthException(ErrorCode.ACCESS_DENIED);
        }

        String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));
        JsonNode payloadNode = objectMapper.readTree(payload);

        if (payloadNode.has("realm_access")
                && payloadNode.get("realm_access").has("roles")) {

            for (JsonNode role : payloadNode.get("realm_access").get("roles")) {
                if (requiredRole.equalsIgnoreCase(role.asText())) {
                    return;
                }
            }
        }
        throw new AuthException(ErrorCode.INCORRECT_ROLE);
    }

    public void logout(String refreshToken) {
        String logoutEndpoint = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", PUBLIC_CLIENT_ID);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            restTemplate.postForEntity(logoutEndpoint, request, String.class);
        } catch (Exception e) {
            throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
        }
    }

    private void register(UserRepresentation user, String role) {
        Keycloak keycloakClient = getKeycloakClient();
        Response response = keycloakClient.realm(realm).users().create(user);
        if (response.getStatus() == 201) {
            String path = response.getLocation().getPath();
            String keycloakUserId = path.substring(path.lastIndexOf("/") + 1);
            RoleRepresentation roleRepresentation = keycloakClient.realm(realm).roles().get(role.toUpperCase()).toRepresentation();

            keycloakClient.realm(realm).users().get(keycloakUserId).roles().realmLevel()
                    .add(Collections.singletonList(roleRepresentation));
            UserRegisteredEvent event = new UserRegisteredEvent(
                    keycloakUserId, user.getEmail(), user.getFirstName(), user.getLastName()
            );

            registrationPublisher.publishUserRegistration(role, event);
        } else {
            log.error("Error while creating user in Keycloak: {}", response.getStatusInfo().getReasonPhrase());
            throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
        }
    }

    public TokenResponseDto refreshAccessToken(String refreshToken) {
        String tokenEndpoint = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", PUBLIC_CLIENT_ID);
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            Map<String, Object> body = response.getBody();

            return new TokenResponseDto(
                    (String) body.get("access_token"),
                    (String) body.get("refresh_token")
            );
        } catch (Exception e) {
            throw new AuthException(ErrorCode.TOKEN_EXPIRED);
        }
    }

    public void changeUserPassword(String userId, String newPassword) {
        Keycloak keycloakClient = getKeycloakClient();

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);

        try {
            keycloakClient.realm(realm).users().get(userId).resetPassword(credential);
            log.info("Zmieniono hasło dla użytkownika Keycloak o ID: {}", userId);
        } catch (Exception e) {
            log.error("Błąd podczas zmiany hasła w Keycloak dla ID: {}", userId, e);
            throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
        }
    }
}
