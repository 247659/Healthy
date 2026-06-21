package healthmonitor.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.auth.model.dto.*;
import healthmonitor.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerPatient_ShouldReturnCreatedAndTokens_WhenDataIsValid() throws Exception {
        PatientRegistrationDto request = new PatientRegistrationDto();
        request.setEmail("john.doe@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("StrongPass123!");

        TokenResponseDto mockTokens = new TokenResponseDto("access-token-123", "refresh-token-123");
        when(authService.login(any(LoginRequestDto.class))).thenReturn(mockTokens);

        mockMvc.perform(post("/api/v1/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));

        verify(authService, times(1)).registerPatient(request);
        verify(authService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    void registerPatient_ShouldReturnBadRequest_WhenPasswordIsTooWeak() throws Exception {
        PatientRegistrationDto request = new PatientRegistrationDto();
        request.setEmail("john.doe@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("weakpass");

        mockMvc.perform(post("/api/v1/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerPatient(any());
    }

    @Test
    void registerPatient_ShouldReturnBadRequest_WhenNameContainsNumbers() throws Exception {
        PatientRegistrationDto request = new PatientRegistrationDto();
        request.setEmail("john.doe@example.com");
        request.setFirstName("John123");
        request.setLastName("Doe");
        request.setPassword("StrongPass123!");

        mockMvc.perform(post("/api/v1/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerPatient(any());
    }

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAreValid() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("user@example.com", "password");
        TokenResponseDto expectedTokens = new TokenResponseDto("access", "refresh");

        when(authService.login(any(LoginRequestDto.class))).thenReturn(expectedTokens);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void loginDoctor_ShouldReturnTokens_WhenCredentialsAreValid() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("doctor@example.com", "password");
        TokenResponseDto expectedTokens = new TokenResponseDto("doc-access", "doc-refresh");

        when(authService.loginDoctor(any(LoginRequestDto.class))).thenReturn(expectedTokens);

        mockMvc.perform(post("/api/v1/auth/loginDoctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("doc-access"));
    }

    @Test
    void logout_ShouldReturnOk() throws Exception {
        LogoutRequestDto logoutRequest = new LogoutRequestDto();
        logoutRequest.setRefreshToken("refresh-token-123");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

        verify(authService, times(1)).logout(logoutRequest);
    }

    @Test
    void refreshToken_ShouldReturnNewTokens() throws Exception {
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken("old-refresh-token");

        TokenResponseDto newTokens = new TokenResponseDto("new-access", "new-refresh");
        when(authService.refresh(any(RefreshTokenRequestDto.class))).thenReturn(newTokens);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void changePassword_ShouldReturnOk() throws Exception {
        String userId = "user-uuid-123";
        Map<String, String> requestBody = Map.of("password", "NewStrongPass123!");

        mockMvc.perform(put("/api/v1/auth/users/{userId}/password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        verify(authService, times(1)).changePassword(userId, "NewStrongPass123!");
    }
}