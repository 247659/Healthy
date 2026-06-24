package healthmonitor.auth.service;

import healthmonitor.auth.model.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandardAuthServiceTest {

    @Mock
    private KeycloakUserService keycloakUserService;

    @InjectMocks
    private StandardAuthService authService;

    @Test
    void registerPatient_ShouldDelegateToKeycloakUserService() {
        // given
        PatientRegistrationDto dto = new PatientRegistrationDto();
        dto.setEmail("test@test.com");
        dto.setPassword("pass");
        dto.setFirstName("Jan");
        dto.setLastName("Kowalski");

        // when
        authService.registerPatient(dto);

        // then
        verify(keycloakUserService, times(1))
                .createUserInKeycloak("test@test.com", "pass", "Jan", "Kowalski", "patient");
    }

    @Test
    void login_ShouldDelegateToKeycloakUserService() {
        // given
        LoginRequestDto request = new LoginRequestDto("test@test.com", "pass");
        TokenResponseDto expectedResponse = new TokenResponseDto("access", "refresh");
        when(keycloakUserService.loginPatient("test@test.com", "pass")).thenReturn(expectedResponse);

        // when
        TokenResponseDto result = authService.login(request);

        // then
        assertEquals(expectedResponse, result);
        verify(keycloakUserService, times(1)).loginPatient("test@test.com", "pass");
    }

    @Test
    void loginDoctor_ShouldDelegateToKeycloakUserService() {
        // given
        LoginRequestDto request = new LoginRequestDto("doc@test.com", "pass");
        TokenResponseDto expectedResponse = new TokenResponseDto("doc-access", "doc-refresh");
        when(keycloakUserService.loginDoctor("doc@test.com", "pass")).thenReturn(expectedResponse);

        // when
        TokenResponseDto result = authService.loginDoctor(request);

        // then
        assertEquals(expectedResponse, result);
        verify(keycloakUserService, times(1)).loginDoctor("doc@test.com", "pass");
    }

    @Test
    void logout_ShouldDelegateToKeycloakUserService() {
        // given
        LogoutRequestDto request = new LogoutRequestDto();
        request.setRefreshToken("refresh-token");

        // when
        authService.logout(request);

        // then
        verify(keycloakUserService, times(1)).logout("refresh-token");
    }

    @Test
    void refresh_ShouldDelegateToKeycloakUserService() {
        // given
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken("old-refresh-token");
        TokenResponseDto expectedResponse = new TokenResponseDto("new-access", "new-refresh");

        when(keycloakUserService.refreshAccessToken("old-refresh-token")).thenReturn(expectedResponse);

        // when
        TokenResponseDto result = authService.refresh(request);

        // then
        assertEquals(expectedResponse, result);
        verify(keycloakUserService, times(1)).refreshAccessToken("old-refresh-token");
    }

    @Test
    void changePassword_ShouldDelegateToKeycloakUserService() {
        // given
        String userId = "user-123";
        String password = "new-password";

        // when
        authService.changePassword(userId, password);

        // then
        verify(keycloakUserService, times(1)).changeUserPassword(userId, password);
    }
}
