package healthmonitor.service;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.model.dto.TokenResponseDto;
import healthmonitor.model.UserRegisteredEvent;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
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

    private final RestTemplate restTemplate = new RestTemplate();
    private final RabbitTemplate rabbitTemplate;

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
            throw new RuntimeException("A user with email address " + email + " already exists!");
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

        if (role.equals("patient")) {
            registerPatient(user);
        }
    }

    public TokenResponseDto login(String email, String password) {
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
            return new TokenResponseDto(
                    (String) body.get("access_token"),
                    (String) body.get("refresh_token")
            );
        } catch (Exception e) {
            throw new RuntimeException("Incorrect login or password");
        }
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
            throw new RuntimeException("Error while logging out");
        }
    }

    private void registerPatient(UserRepresentation user) {
        Keycloak keycloakClient = getKeycloakClient();
        Response response = keycloakClient.realm(realm).users().create(user);

        if (response.getStatus() == 201) {
            String path = response.getLocation().getPath();
            String keycloakUserId = path.substring(path.lastIndexOf('/') + 1);

            RoleRepresentation patientRole = keycloakClient.realm(realm).roles().get("PATIENT").toRepresentation();

            keycloakClient.realm(realm).users().get(keycloakUserId).roles().realmLevel()
                    .add(Collections.singletonList(patientRole));
            UserRegisteredEvent event = new UserRegisteredEvent(
                    keycloakUserId, user.getEmail(), user.getFirstName(), user.getLastName()
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.AUTH_EXCHANGE, "user.registered.patient", event);
        } else {
            throw new RuntimeException("Error while creating user in Keycloak: " + response.getStatusInfo().getReasonPhrase());
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
            throw new RuntimeException("Your session has expired. Please log in again.");
        }
    }
}
