package healthmonitor.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.auth.Client.KeycloakClient;
import healthmonitor.auth.exception.AuthException;
import healthmonitor.auth.exception.ErrorCode;
import healthmonitor.auth.model.UserRegisteredEvent;
import healthmonitor.auth.model.dto.TokenResponseDto;
import healthmonitor.auth.publisher.RegistrationPublisher;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserService {

    @Value("${keycloak.realm}")
    private String realm;

    private final KeycloakClient keycloakClient;
    private final Keycloak keycloakAuthClient;
    private final ObjectMapper objectMapper;
    private final RegistrationPublisher registrationPublisher;

    public void createUserInKeycloak(String email, String password, String firstName, String lastName, String role) {
        List<UserRepresentation> existingUsers = keycloakAuthClient.realm(realm)
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
        TokenResponseDto tokens = keycloakClient.fetchToken(email, password);
        try {
            validateRole(tokens.getAccessToken(), requiredRole);
        } catch (Exception e) {
            keycloakClient.logout(tokens.getRefreshToken());
            throw new AuthException(ErrorCode.INCORRECT_CREDENTIALS);
        }

        return tokens;
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
        keycloakClient.refreshToken(refreshToken);
    }

    private void register(UserRepresentation user, String role) {
        String keycloakUserId = null;
        try {
            Response response = keycloakAuthClient.realm(realm).users().create(user);
            if (response.getStatus() != 201) {
                log.error("Error while creating user in Keycloak: {}", response.getStatusInfo().getReasonPhrase());
                throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
            }

            String path = response.getLocation().getPath();
            keycloakUserId = path.substring(path.lastIndexOf("/") + 1);
            RoleRepresentation roleRepresentation = keycloakAuthClient.realm(realm).roles().get(role.toUpperCase()).toRepresentation();

            keycloakAuthClient.realm(realm).users().get(keycloakUserId).roles().realmLevel()
                    .add(Collections.singletonList(roleRepresentation));
            UserRegisteredEvent event = new UserRegisteredEvent(
                    keycloakUserId, user.getEmail(), user.getFirstName(), user.getLastName()
            );

            registrationPublisher.publishUserRegistration(role, event);
        } catch (Exception e) {
            log.error("Błąd podczas rejestracji użytkownika: {}", user.getEmail(), e);

            if (keycloakUserId != null) {
                try {
                    log.warn("Wycofuję (rollback) użytkownika z Keycloak z powodu błędu: {}", keycloakUserId);
                    keycloakAuthClient.realm(realm).users().get(keycloakUserId).remove();
                } catch (Exception rollbackEx) {
                    log.error("KRYTYCZNE: Nie udało się usunąć osieroconego użytkownika z Keycloak: {}", keycloakUserId, rollbackEx);
                }
            }

            if (e instanceof AuthException) {
                throw (AuthException) e;
            }
            throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
        }
    }

    public TokenResponseDto refreshAccessToken(String refreshToken) {
        return keycloakClient.refreshToken(refreshToken);
    }

    public void changeUserPassword(String userId, String newPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);

        try {
            keycloakAuthClient.realm(realm).users().get(userId).resetPassword(credential);
            log.info("Zmieniono hasło dla użytkownika Keycloak o ID: {}", userId);
        } catch (Exception e) {
            log.error("Błąd podczas zmiany hasła w Keycloak dla ID: {}", userId, e);
            throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
        }
    }
}
