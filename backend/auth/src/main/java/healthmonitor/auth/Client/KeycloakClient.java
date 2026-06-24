package healthmonitor.auth.Client;

import healthmonitor.auth.exception.AuthException;
import healthmonitor.auth.exception.ErrorCode;
import healthmonitor.auth.model.dto.TokenResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KeycloakClient {

    private final RestClient restClient;
    private final String realmUrl;
    private final String publicClientId = "health-api";

    public KeycloakClient(RestClient restClient,
                               @Value("${keycloak.server-url}") String serverUrl,
                               @Value("${keycloak.realm}") String realm) {
        this.restClient = restClient;
        this.realmUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect";
    }
    public TokenResponseDto fetchToken(String email, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", publicClientId);
        formData.add("username", email);
        formData.add("password", password);
        formData.add("grant_type", "password");

        Map<String, Object> response = restClient.post()
                .uri(realmUrl + "/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new AuthException(ErrorCode.INCORRECT_CREDENTIALS);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {});

        return new TokenResponseDto(
                (String) response.get("access_token"),
                (String) response.get("refresh_token")
        );
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", publicClientId);
        formData.add("refresh_token", refreshToken);

        restClient.post()
                .uri(realmUrl + "/logout")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AuthException(ErrorCode.INTERVAL_SERVER_ERROR);
                })
                .toBodilessEntity();
    }

    public TokenResponseDto refreshToken(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", publicClientId);
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);

        Map<String, Object> response = restClient.post()
                .uri(realmUrl + "/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AuthException(ErrorCode.TOKEN_EXPIRED);
                })
                .body(new ParameterizedTypeReference<>() {});

        return new TokenResponseDto(
                (String) response.get("access_token"),
                (String) response.get("refresh_token")
        );
    }
}
